package io.github.tt432.eyelib.client.instrument.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentDatabaseTest {

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = InstrumentDatabase.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM performance_events WHERE event_type IN ('TEST', 'PERF_TEST')");
        }
    }

    @AfterEach
    void tearDown() {
        InstrumentDatabase.getInstance().close();
    }

    @Test
    void testSchemaCreation() throws Exception {
        // given / when
        Connection conn = InstrumentDatabase.getInstance().getConnection();
        InstrumentDatabase.getInstance().ensureSchema();

        // then — verify the table exists via JDBC metadata
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet tables = meta.getTables(null, null, "PERFORMANCE_EVENTS", null)) {
            assertTrue(tables.next(), "Table PERFORMANCE_EVENTS should exist");
        }

        // verify index exists
        try (ResultSet indexes = meta.getIndexInfo(null, null, "PERFORMANCE_EVENTS", false, false)) {
            boolean foundIndex = false;
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if ("IDX_EVENT_TYPE_TIME".equalsIgnoreCase(indexName)) {
                    foundIndex = true;
                    break;
                }
            }
            assertTrue(foundIndex, "Index IDX_EVENT_TYPE_TIME should exist on PERFORMANCE_EVENTS");
        }
    }

    @Test
    void testConnectionReopen() throws Exception {
        // given — open connection
        Connection conn1 = InstrumentDatabase.getInstance().getConnection();
        assertNotNull(conn1);
        assertFalse(conn1.isClosed());

        // when — close and reopen
        InstrumentDatabase.getInstance().close();
        Connection conn2 = InstrumentDatabase.getInstance().getConnection();

        // then — new connection should be valid
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
    }

    @Test
    void testDataPersistence() throws Exception {
        // given — insert a row
        Connection conn = InstrumentDatabase.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO performance_events (event_type, source, metric_name, metric_value, metric_unit, thread_name) " +
                    "VALUES ('TEST', 'InstrumentDatabaseTest', 'test_metric', 42.0, 'ms', 'main')");
        }

        // when — close, reopen, and query
        InstrumentDatabase.getInstance().close();
        Connection reopened = InstrumentDatabase.getInstance().getConnection();

        // then — data should persist
        try (Statement stmt = reopened.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT event_type, metric_name, metric_value FROM performance_events WHERE event_type = 'TEST'")) {

            assertTrue(rs.next(), "Should find the inserted row after reconnect");
            assertEquals("TEST", rs.getString("event_type"));
            assertEquals("test_metric", rs.getString("metric_name"));
            assertEquals(42.0, rs.getDouble("metric_value"), 0.001);
        }
    }

    @Test
    void testInsert10kEventsAndQuery() throws Exception {
        Connection conn = InstrumentDatabase.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO performance_events (event_type, source, metric_name, metric_value, metric_unit, thread_name) " +
                            "VALUES (?, ?, ?, ?, ?, ?)")) {
                for (int i = 0; i < 10_000; i++) {
                    ps.setString(1, "PERF_TEST");
                    ps.setString(2, "InstrumentDatabaseTest");
                    ps.setString(3, "batch_metric");
                    ps.setDouble(4, i);
                    ps.setString(5, "ms");
                    ps.setString(6, "test-thread");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM performance_events WHERE event_type = 'PERF_TEST'")) {
                assertTrue(rs.next());
                assertEquals(10_000, rs.getInt(1));
            }
        } finally {
            try (Statement cleanup = conn.createStatement()) {
                cleanup.executeUpdate("DELETE FROM performance_events WHERE event_type = 'PERF_TEST'");
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    @Test
    void testConcurrentAccessSafety() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS), "Start latch timed out");

                    Connection conn = InstrumentDatabase.getInstance().getConnection();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1));
                    }

                    return null;
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Workers did not become ready in time");
            start.countDown();

            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testCorruptionRecovery() throws Exception {
        InstrumentDatabase.getInstance().close();

        Path dbFile = Paths.get("eyelib_instrument.mv.db").toAbsolutePath();
        Path traceFile = Paths.get("eyelib_instrument.trace.db").toAbsolutePath();
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(traceFile);

        Connection reopened = InstrumentDatabase.getInstance().getConnection();
        assertNotNull(reopened);
        assertFalse(reopened.isClosed());

        try (Statement stmt = reopened.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        assertTrue(Files.exists(dbFile), "Database file should be recreated after deletion");
    }
}
