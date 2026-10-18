package com.cybermonitor;

import com.cybermonitor.config.AppConfig;
import com.cybermonitor.service.StartupCheckService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void springContextLoadsAndSqliteConnects(@TempDir Path tempDir) {
        Map<String, Object> testProperties =
                Map.of(AppConfig.DB_PATH_PROPERTY, tempDir.resolve("test.db").toString());

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("test", testProperties));
            context.register(AppConfig.class);
            context.refresh();

            assertTrue(context.getBean(StartupCheckService.class).checkDatabase());
        }
    }
}
