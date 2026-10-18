package com.cybermonitor;

import com.cybermonitor.repository.sqlite.SqliteConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteConnectionFactoryTest {

    @Test
    void opensConnectionAndAnswersSelectOne(@TempDir Path tempDir) throws Exception {
        SqliteConnectionFactory factory = new SqliteConnectionFactory(tempDir.resolve("test.db"));

        try (Connection connection = factory.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void createsMissingParentFolders(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("data").resolve("nested").resolve("test.db");

        new SqliteConnectionFactory(dbFile);

        assertTrue(Files.isDirectory(dbFile.getParent()));
    }
}
