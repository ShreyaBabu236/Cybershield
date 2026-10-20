package com.cybermonitor.repository.sqlite;

import com.cybermonitor.repository.DataResetRepository;

import java.sql.Statement;
import java.util.List;
import java.util.Objects;

public class SqliteDataResetRepository implements DataResetRepository {

    /** Every table in schema.sql. The table names are fixed constants, never user input. */
    static final List<String> TABLES = List.of("incident_root_causes", "incident_alerts", "incidents",
            "alert_evidence", "alerts", "events", "imported_files");

    private final SqliteConnectionFactory connections;

    public SqliteDataResetRepository(SqliteConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections must not be null");
    }

    @Override
    public void deleteAll() {
        Transactions.inTransaction(connections, "clearing all data", connection -> {
            try (Statement statement = connection.createStatement()) {
                for (String table : TABLES) {
                    statement.executeUpdate("DELETE FROM " + table);
                }
            }
        });
    }
}
