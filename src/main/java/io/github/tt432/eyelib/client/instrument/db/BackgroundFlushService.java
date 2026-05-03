package io.github.tt432.eyelib.client.instrument.db;

import io.github.tt432.eyelib.client.instrument.event.EventRingBuffer;
import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton service that batch-flushes {@link InstrumentEvent}s from the
 * {@link EventRingBuffer} into the H2-backed {@link InstrumentDatabase}.
 * <p>
 * A single daemon thread polls the ring buffer on a fixed interval and writes
 * events via a batched {@link PreparedStatement}.  If the database write fails
 * the error is logged and execution continues — the render thread is never
 * blocked.
 * <p>
 * Lifecycle:
 * <ul>
 *   <li>{@link #install()} — start the background scheduler</li>
 *   <li>{@link #shutdown()} — stop the scheduler, drain remaining events, close DB</li>
 * </ul>
 * <p>
 * Singleton access follows the <em>initialization-on-demand holder</em> idiom,
 * matching {@code EventRingBuffer}, {@code InstrumentDatabase}, and
 * {@code BackgroundFlushService}.
 */
public final class BackgroundFlushService {

    private static final Logger LOG = Logger.getLogger(BackgroundFlushService.class.getName());

    /** Maximum number of events to drain in a single flush cycle. */
    private static final int BATCH_SIZE = 500;

    /** Interval (in seconds) between scheduled flushes. */
    private static final int FLUSH_INTERVAL_SECONDS = 1;

    private volatile @Nullable ScheduledExecutorService executor;
    private final EventRingBuffer ringBuffer;
    private final InstrumentDatabase database;

    private static final class Holder {
        static final BackgroundFlushService INSTANCE = new BackgroundFlushService(
                EventRingBuffer.getInstance(), InstrumentDatabase.getInstance());
    }

    private BackgroundFlushService(final EventRingBuffer ringBuffer, final InstrumentDatabase database) {
        this.ringBuffer = ringBuffer;
        this.database = database;
    }

    /**
     * Returns the singleton {@code BackgroundFlushService} instance.
     *
     * @return the shared service
     */
    public static BackgroundFlushService getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Starts the background flush scheduler.
     * <p>
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    public void install() {
        if (executor != null) return;
        final ScheduledExecutorService createdExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "eyelib-instrument-flush");
            t.setDaemon(true);
            return t;
        });
        executor = createdExecutor;
        createdExecutor.scheduleAtFixedRate(this::flush,
                FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        LOG.info("BackgroundFlushService installed — flushing every " + FLUSH_INTERVAL_SECONDS + "s");
    }

    /**
     * Drains up to {@link #BATCH_SIZE} events from the ring buffer and inserts
     * them into the database in a single batched transaction.
     * <p>
     * This method is safe to call from any thread (including the test thread)
     * and is idempotent when the buffer is empty.
     */
    void flush() {
        if (ringBuffer.isEmpty()) return;

        final List<InstrumentEvent> batch = new ArrayList<>(BATCH_SIZE);
        InstrumentEvent event;
        while ((event = ringBuffer.poll()) != null && batch.size() < BATCH_SIZE) {
            batch.add(event);
        }

        if (batch.isEmpty()) return;

        final String sql = "INSERT INTO performance_events "
                + "(event_type, source, metric_name, metric_value, metric_unit, thread_name, extra_json) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (final InstrumentEvent e : batch) {
                    ps.setString(1, e.eventType());
                    ps.setString(2, e.source());
                    ps.setString(3, e.metricName());
                    ps.setDouble(4, e.metricValue());
                    ps.setString(5, e.metricUnit());
                    ps.setString(6, Thread.currentThread().getName());
                    ps.setString(7, e.extraJson());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (final SQLException ignored) {
                    // Best-effort rollback — nothing more we can do
                }
                throw ex;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (final SQLException ignored) {
                    // Best-effort reset
                }
            }
        } catch (final SQLException ex) {
            LOG.log(Level.WARNING, "Failed to flush " + batch.size() + " events", ex);
            // Graceful degradation: render path must never crash from a DB write failure
        }
    }

    /**
     * Gracefully shuts down the background scheduler, drains any remaining
     * buffered events, and closes the database connection.
     * <p>
     * Safe to call multiple times — subsequent calls are no-ops once the
     * executor has been torn down.
     */
    public void shutdown() {
        final ScheduledExecutorService runningExecutor = executor;
        if (runningExecutor == null) return;
        runningExecutor.shutdown();
        try {
            if (!runningExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                runningExecutor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            runningExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executor = null;
        }
        // Drain any events that arrived after the last scheduled flush
        flush();
        database.close();
        LOG.info("BackgroundFlushService shut down");
    }
}
