package io.github.tt432.eyelib.client.instrument.event;

import org.jspecify.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lock-free, singleton event ring buffer for hot-path zero-allocation
 * instrument event recording.
 * <p>
 * Uses a {@link ConcurrentLinkedQueue} internally — all operations are
 * non-blocking and wait-free, making this safe for use on the render thread
 * or any latency-sensitive path.  No allocations occur inside {@link #offer}
 * beyond the caller-provided {@link InstrumentEvent} instance.
 * <p>
 * When the number of buffered events exceeds {@link #SOFT_CAPACITY} the
 * oldest events are silently dropped to prevent runaway memory growth.
 * <p>
 * Singleton access via {@link #getInstance()} follows the
 * <em>initialization-on-demand holder</em> pattern (same as
 * {@code ManagerEventPublishBridge}).
 *
 * @see InstrumentEvent
 */
public final class EventRingBuffer {

    private static final Logger LOG = Logger.getLogger(EventRingBuffer.class.getName());

    /**
     * Soft capacity limit.  When the queue exceeds this threshold the oldest
     * events are drained on each subsequent {@link #offer(InstrumentEvent)}.
     */
    static final int SOFT_CAPACITY = 100_000;

    private final Queue<InstrumentEvent> queue = new ConcurrentLinkedQueue<>();

    /**
     * Package-private constructor for testing.  Production code should obtain
     * the singleton via {@link #getInstance()}.
     */
    EventRingBuffer() {
    }

    /**
     * Initialization-on-demand holder idiom.
     */
    private static final class Holder {
        static final EventRingBuffer INSTANCE = new EventRingBuffer();
    }

    /**
     * Returns the singleton {@code EventRingBuffer} instance.
     *
     * @return the shared event ring buffer
     */
    public static EventRingBuffer getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Non-blocking offer for hot-path recording.
     * <p>
     * If the buffer exceeds the soft capacity, oldest events are dropped
     * (one per offer until the size is back within the limit).  A warning
     * is logged on each drop.
     *
     * @param event the instrument event to record; must not be {@code null}
     */
    public void offer(final InstrumentEvent event) {
        queue.offer(event);
        // Enforce soft capacity by draining oldest if exceeded
        while (queue.size() > SOFT_CAPACITY) {
            final InstrumentEvent dropped = queue.poll();
            if (dropped != null) {
                LOG.log(Level.WARNING,
                        "EventRingBuffer exceeded soft capacity of {0}, dropping oldest event [{1}]",
                        new Object[]{SOFT_CAPACITY, dropped.eventType()});
            }
        }
    }

    /**
     * Non-blocking poll.  Returns the oldest buffered event or {@code null}
     * if the buffer is empty.
     *
     * @return the next available event, or {@code null}
     */
    public @Nullable InstrumentEvent poll() {
        return queue.poll();
    }

    /**
     * Returns the approximate number of events currently buffered.
     * <p>
     * Note: because the queue is concurrent, the returned value is a
     * best-effort snapshot that may change immediately.
     *
     * @return approximate event count
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns {@code true} if the buffer is empty (best-effort snapshot).
     *
     * @return true if no events are buffered
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
