package io.github.tt432.eyelib.client.instrument.db;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class InstrumentDatabase {
    private static final Logger LOG = Logger.getLogger(InstrumentDatabase.class.getName());
    private static final String DB_URL = "jdbc:h2:file:./eyelib_instrument;ACCESS_MODE_DATA=rws;WRITE_DELAY=1000";

    private @Nullable Connection connection;

    private static final class Holder {
        static final InstrumentDatabase INSTANCE = new InstrumentDatabase();
    }

    private InstrumentDatabase() {}

    public static InstrumentDatabase getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        Connection existing = connection;
        if (existing == null || existing.isClosed()) {
            existing = DriverManager.getConnection(DB_URL);
            connection = existing;
            LOG.info("H2 database connection opened: " + DB_URL);
            ensureSchema();
        }
        return existing;
    }

    public void ensureSchema() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS performance_events (
                    id BIGINT AUTO_INCREMENT,
                    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
                    event_type VARCHAR(64) NOT NULL,
                    source VARCHAR(128),
                    metric_name VARCHAR(64),
                    metric_value DOUBLE,
                    metric_unit VARCHAR(16),
                    thread_name VARCHAR(128),
                    extra_json VARCHAR(4096)
                )
                """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_event_type_time ON performance_events(event_type, event_time)");
            LOG.info("Database schema ensured");
        }
    }

    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOG.info("H2 database connection closed");
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
}
