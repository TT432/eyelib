package io.github.tt432.eyelib.client.instrument.db;

import io.github.tt432.eyelib.client.instrument.event.EventRingBuffer;
import io.github.tt432.eyelib.client.instrument.event.InstrumentEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BackgroundFlushService}.
 * <p>
 * All tests use the shared H2 database file ({@code ./eyelib_instrument}) and
 * clean up after themselves by deleting test rows in {@link #setUp()}.
 */
class BackgroundFlushServiceTest {

    @BeforeEach
    void setUp() throws Exception {
        // Drain any leftover events from previous tests
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        while (buffer.poll() != null) {
            // drain
        }

        // Clean the performance_events table so each test starts from empty
        try (Connection conn = InstrumentDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM performance_events");
        }
    }

    @AfterEach
    void tearDown() {
        BackgroundFlushService.getInstance().shutdown();
        InstrumentDatabase.getInstance().close();
    }

    @Test
    void testBatchFlush() throws Exception {
        // given — offer 100 events to the ring buffer
        EventRingBuffer buffer = EventRingBuffer.getInstance();
        for (int i = 0; i < 100; i++) {
            buffer.offer(new InstrumentEvent(
                    "TEST_FLUSH_BATCH", "BackgroundFlushServiceTest",
                    "metric_" + i, i * 1.0, "count", null));
        }

        // when — flush directly (no scheduler dependency)
        BackgroundFlushService.getInstance().flush();

        // then — 100 rows should be persisted
        try (Connection conn = InstrumentDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM performance_events WHERE event_type = 'TEST_FLUSH_BATCH'")) {
            assertTrue(rs.next(), "Expected a result row from COUNT query");
            assertEquals(100, rs.getInt("cnt"),
                    "All 100 offered events should have been flushed");
        }
    }

    @Test
    void testShutdownDrain() throws Exception {
        // given — install the service and offer 50 events before the scheduler fires
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        service.install();

        EventRingBuffer buffer = EventRingBuffer.getInstance();
        for (int i = 0; i < 50; i++) {
            buffer.offer(new InstrumentEvent(
                    "TEST_DRAIN", "BackgroundFlushServiceTest",
                    "drain_metric_" + i, i * 2.0, "ms", "{}"));
        }

        // when — shutdown drains remaining events (FLUSH_INTERVAL_SECONDS = 1,
        // so the scheduled flush has not yet fired)
        service.shutdown();

        // then — all 50 events should have been flushed during shutdown drain
        try (Connection conn = InstrumentDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM performance_events WHERE event_type = 'TEST_DRAIN'")) {
            assertTrue(rs.next(), "Expected a result row from COUNT query");
            assertEquals(50, rs.getInt("cnt"),
                    "All 50 offered events should have been drained on shutdown");
        }
    }

    @Test
    void testEmptyBufferNoFlush() {
        // given — empty ring buffer and installed service
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        service.install();

        // when / then — shutdown with empty buffer must not throw
        assertDoesNotThrow(service::shutdown,
                "Shutdown with empty buffer should complete without exception");
    }

    @Test
    void testMultipleFlushCyclesAccumulate() throws Exception {
        // given — two separate flushes without scheduler
        BackgroundFlushService service = BackgroundFlushService.getInstance();
        EventRingBuffer buffer = EventRingBuffer.getInstance();

        // first batch: 10 events
        for (int i = 0; i < 10; i++) {
            buffer.offer(new InstrumentEvent(
                    "TEST_MULTI", "BackgroundFlushServiceTest",
                    "first_" + i, i, "count", null));
        }
        service.flush();

        // second batch: 20 events
        for (int i = 0; i < 20; i++) {
            buffer.offer(new InstrumentEvent(
                    "TEST_MULTI", "BackgroundFlushServiceTest",
                    "second_" + i, i * 2, "count", null));
        }
        service.flush();

        // then — all 30 rows should be persisted
        try (Connection conn = InstrumentDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM performance_events WHERE event_type = 'TEST_MULTI'")) {
            assertTrue(rs.next(), "Expected a result row from COUNT query");
            assertEquals(30, rs.getInt("cnt"),
                    "Accumulated events across multiple flush cycles should be persisted");
        }
    }

    @Test
    void testConsecutiveInstallShutdownCycles() {
        // given — fresh service
        BackgroundFlushService service = BackgroundFlushService.getInstance();

        // when — install + shutdown twice
        service.install();
        service.shutdown();

        // second cycle must also succeed (executor was reset to null)
        service.install();
        service.shutdown();

        // then — no exception thrown through both cycles
        assertDoesNotThrow(service::shutdown,
                "A third shutdown (after reinstall) should also be safe");
    }
}
