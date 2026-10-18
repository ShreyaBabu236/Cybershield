package com.cybermonitor.repository.sqlite;

import com.cybermonitor.exception.CyberShieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Opens JDBC connections to the local SQLite database file.
 *
 * <p>Each call to {@link #openConnection()} returns a new connection that the
 * caller must close (use try-with-resources). SQLite file connections are cheap,
 * so no connection pool is needed for this project.
 */
public class SqliteConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(SqliteConnectionFactory.class);

    private final String jdbcUrl;

    public SqliteConnectionFactory(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile must not be null");
        Path absoluteFile = databaseFile.toAbsolutePath();
        try {
            Path folder = absoluteFile.getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            throw new CyberShieldException("Cannot create database folder for " + absoluteFile, e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + absoluteFile;
        log.info("SQLite database file: {}", absoluteFile);
    }

    public Connection openConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new CyberShieldException("Cannot open SQLite connection: " + jdbcUrl, e);
        }
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }
}
