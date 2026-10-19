package com.cybermonitor.ui;

import com.cybermonitor.export.CsvExporter;
import com.cybermonitor.parser.EventParser;
import com.cybermonitor.service.AlertService;
import com.cybermonitor.service.IncidentService;
import com.cybermonitor.service.MaintenanceService;
import com.cybermonitor.service.PipelineResult;
import com.cybermonitor.service.PipelineService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * The main window: a toolbar to import a log file, and tabs for alerts and incidents.
 * The equipment track adds its own tabs (import screen, dashboard) to {@link #tabs()}.
 */
public class MainWindow extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final PipelineService pipeline;
    private final MaintenanceService maintenance;
    private final AlertsView alertsView;
    private final IncidentsView incidentsView;
    private final TabPane tabs = new TabPane();
    private final ComboBox<EventParser> parserBox = new ComboBox<>();
    private final Button importButton = new Button("Import file...");
    private final Button clearButton = new Button("Clear data...");
    private final Label status = new Label();

    public MainWindow(PipelineService pipeline, AlertService alertService, IncidentService incidentService,
                      CsvExporter exporter, List<EventParser> parsers, MaintenanceService maintenance,
                      String databaseStatus) {
        this.pipeline = pipeline;
        this.maintenance = maintenance;
        this.alertsView = new AlertsView(alertService, exporter);
        this.incidentsView = new IncidentsView(incidentService, exporter);

        parserBox.getItems().addAll(parsers);
        if (!parsers.isEmpty()) {
            parserBox.setValue(parsers.get(0));
        }
        parserBox.setConverter(new javafx.util.StringConverter<EventParser>() {
            @Override
            public String toString(EventParser parser) {
                return parser == null ? "" : parser.formatName();
            }

            @Override
            public EventParser fromString(String text) {
                return null;
            }
        });
        importButton.setOnAction(e -> chooseAndImport());
        clearButton.setOnAction(e -> confirmAndClear());

        Label title = new Label("CyberShield Desktop");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(10, title, new Label("Format"), parserBox, importButton, clearButton, status, spacer,
                new Label(databaseStatus));
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Tab alertsTab = new Tab("Alerts", alertsView);
        Tab incidentsTab = new Tab("Incidents", incidentsView);
        alertsTab.setClosable(false);
        incidentsTab.setClosable(false);
        tabs.getTabs().addAll(alertsTab, incidentsTab);

        setTop(toolbar);
        setCenter(tabs);
    }

    /** Lets other screens add their own tabs. */
    public TabPane tabs() {
        return tabs;
    }

    private void chooseAndImport() {
        EventParser parser = parserBox.getValue();
        if (parser == null) {
            UiSupport.error("No file format is available.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import " + parser.formatName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log, text and CSV files",
                "*.log", "*.txt", "*.csv"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        runImport(file, parser);
    }

    private void confirmAndClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all events, alerts, incidents and notes? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Clear all data");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            maintenance.clearAllData();
            status.setText("All data cleared");
            alertsView.refresh();
            incidentsView.refresh();
        } catch (RuntimeException e) {
            log.error("Clearing data failed", e);
            UiSupport.error("Cannot clear data: " + e.getMessage());
        }
    }

    private void runImport(File file, EventParser parser) {
        importButton.setDisable(true);
        status.setText("Importing " + file.getName() + "...");
        Task<PipelineResult> task = new Task<PipelineResult>() {
            @Override
            protected PipelineResult call() {
                return pipeline.importFile(file.toPath(), parser);
            }
        };
        task.setOnSucceeded(e -> {
            PipelineResult result = task.getValue();
            importButton.setDisable(false);
            status.setText(result.summary());
            if (result.alreadyImported()) {
                UiSupport.info("This file was already imported, so it was skipped.\n\n"
                        + "Use \"Clear data...\" first if you want to import it again.");
                return;
            }
            alertsView.refresh();
            incidentsView.refresh();
            if (result.imported().malformedLines() > 0) {
                UiSupport.info(result.imported().malformedLines() + " malformed lines were skipped.\n\n"
                        + String.join("\n", result.imported().malformedSamples()));
            }
        });
        task.setOnFailed(e -> {
            importButton.setDisable(false);
            status.setText("Import failed");
            Throwable cause = task.getException();
            log.error("Import failed", cause);
            UiSupport.error("Import failed: " + (cause == null ? "unknown error" : cause.getMessage()));
        });
        Thread thread = new Thread(task, "import-" + file.getName());
        thread.setDaemon(true);
        thread.start();
    }
}
