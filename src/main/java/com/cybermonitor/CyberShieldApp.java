package com.cybermonitor;

import com.cybermonitor.config.AppConfig;
import com.cybermonitor.service.StartupCheckService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * JavaFX entry point. Starts the Spring context, checks the database and shows a window.
 */
public class CyberShieldApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(CyberShieldApp.class);

    private AnnotationConfigApplicationContext context;
    private boolean databaseConnected;

    @Override
    public void init() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        log.info("Spring context started");
        databaseConnected = context.getBean(StartupCheckService.class).checkDatabase();
    }

    @Override
    public void start(Stage stage) {
        Label title = new Label("CyberShield Desktop");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label spring = new Label("Spring context: started");
        Label database = new Label("SQLite: " + (databaseConnected ? "connected" : "FAILED (see log)"));

        VBox root = new VBox(10, title, spring, database);
        root.setPadding(new Insets(24));

        stage.setTitle("CyberShield Desktop");
        stage.setScene(new Scene(root, 640, 360));
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
            log.info("Spring context closed");
        }
    }
}
