package com.cybermonitor.service;

import com.cybermonitor.exception.CyberShieldException;
import com.cybermonitor.repository.sqlite.SqliteConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Startup health check: confirms that the SQLite database can be opened and queried.
 */
public class StartupCheckService {

    private static final Logger log = LoggerFactory.getLogger(StartupCheckService.class);

    private final SqliteConnectionFactory connections;

    public StartupCheckService(SqliteConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections must not be null");
    }

    /**
     * Opens a connection and runs {@code SELECT 1}.
     *
     * @return true if SQLite answered correctly, false if anything failed (the reason is logged)
     */
    public boolean checkDatabase() {
        try (Connection connection = connections.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            boolean ok = result.next() && result.getInt(1) == 1;
            if (ok) {
                log.info("SQLite connected");
            } else {
                log.error("SQLite answered, but SELECT 1 did not return 1");
            }
            return ok;
        } catch (SQLException | CyberShieldException e) {
            log.error("SQLite check failed", e);
            return false;
        }
    }
}
