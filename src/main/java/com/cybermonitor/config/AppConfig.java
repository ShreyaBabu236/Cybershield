package com.cybermonitor.config;

import com.cybermonitor.repository.sqlite.SqliteConnectionFactory;
import com.cybermonitor.service.StartupCheckService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

/**
 * Spring configuration: creates the shared objects and wires them together.
 *
 * <p>The database file defaults to {@code cybershield.db} in the working directory.
 * Override it with the JVM option {@code -Dcybershield.db.path=...}.
 */
@Configuration
public class AppConfig {

    public static final String DB_PATH_PROPERTY = "cybershield.db.path";
    public static final String DEFAULT_DB_PATH = "cybershield.db";

    @Bean
    public SqliteConnectionFactory sqliteConnectionFactory(Environment environment) {
        String dbPath = environment.getProperty(DB_PATH_PROPERTY, DEFAULT_DB_PATH);
        return new SqliteConnectionFactory(Path.of(dbPath));
    }

    @Bean
    public StartupCheckService startupCheckService(SqliteConnectionFactory connections) {
        return new StartupCheckService(connections);
    }
}
