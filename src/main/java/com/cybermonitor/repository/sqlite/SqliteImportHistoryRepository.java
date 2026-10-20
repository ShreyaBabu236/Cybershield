package com.cybermonitor.repository.sqlite;

import com.cybermonitor.exception.CyberShieldException;
import com.cybermonitor.repository.ImportHistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

public class SqliteImportHistoryRepository implements ImportHistoryRepository {

    private final SqliteConnectionFactory connections;

    public SqliteImportHistoryRepository(SqliteConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections must not be null");
    }

    @Override
    public boolean contains(String fingerprint) {
        try (Connection connection = connections.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM imported_files WHERE fingerprint = ?")) {
            statement.setString(1, fingerprint);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new CyberShieldException("Database error while reading import history", e);
        }
    }

    @Override
    public void record(String fingerprint, String fileName, Instant importedAt) {
        Transactions.inTransaction(connections, "recording an import", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO imported_files (fingerprint, file_name, imported_at) VALUES (?, ?, ?)")) {
                statement.setString(1, fingerprint);
                statement.setString(2, fileName);
                statement.setLong(3, importedAt.toEpochMilli());
                statement.executeUpdate();
            }
        });
    }
}
