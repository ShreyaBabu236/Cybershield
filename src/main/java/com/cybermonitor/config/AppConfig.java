package com.cybermonitor.config;

import com.cybermonitor.anomaly.AnomalyDetector;
import com.cybermonitor.anomaly.RollingZScoreAnomalyDetector;
import com.cybermonitor.correlation.CorrelationEngine;
import com.cybermonitor.correlation.PrecedenceRootCauseRanker;
import com.cybermonitor.correlation.PrecedenceStatistics;
import com.cybermonitor.correlation.RootCauseRanker;
import com.cybermonitor.correlation.TimeWindowCorrelationEngine;
import com.cybermonitor.detection.BruteForceRule;
import com.cybermonitor.detection.DefaultDetectionEngine;
import com.cybermonitor.detection.DetectionEngine;
import com.cybermonitor.detection.DetectionRule;
import com.cybermonitor.detection.FailThenSuccessRule;
import com.cybermonitor.detection.TelemetryThresholdRule;
import com.cybermonitor.export.CsvExporter;
import com.cybermonitor.model.Severity;
import com.cybermonitor.parser.AuthLogParser;
import com.cybermonitor.parser.EquipmentAlarmCsvParser;
import com.cybermonitor.repository.AlertRepository;
import com.cybermonitor.repository.EventRepository;
import com.cybermonitor.repository.DataResetRepository;
import com.cybermonitor.repository.ImportHistoryRepository;
import com.cybermonitor.repository.IncidentRepository;
import com.cybermonitor.repository.sqlite.SchemaInitializer;
import com.cybermonitor.repository.sqlite.SqliteAlertRepository;
import com.cybermonitor.repository.sqlite.SqliteConnectionFactory;
import com.cybermonitor.repository.sqlite.SqliteDataResetRepository;
import com.cybermonitor.repository.sqlite.SqliteEventRepository;
import com.cybermonitor.repository.sqlite.SqliteImportHistoryRepository;
import com.cybermonitor.repository.sqlite.SqliteIncidentRepository;
import com.cybermonitor.repository.sqlite.SqliteRootCauseCandidateStore;
import com.cybermonitor.service.AlertService;
import com.cybermonitor.service.DefaultIncidentService;
import com.cybermonitor.service.IncidentService;
import com.cybermonitor.service.LogImportService;
import com.cybermonitor.service.MaintenanceService;
import com.cybermonitor.service.PipelineService;
import com.cybermonitor.service.StartupCheckService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spring configuration: creates the shared objects and wires them together.
 *
 * <p>The database file defaults to {@code cybershield.db} in the working directory.
 * Override it with the JVM option {@code -Dcybershield.db.path=...}. Detection and correlation
 * settings live in {@code cybershield.properties}.
 *
 * <p>Parsers are collected automatically: to add one, define one more {@code @Bean}. The equipment
 * track's telemetry rule, z-score detector and root-cause ranker have no built-in defaults (by design),
 * so they are created and used only when their {@code cybershield.telemetry.*}, {@code cybershield.anomaly.*}
 * and {@code cybershield.root-cause.*} properties are set (see docs/data-formats.md).
 */
@Configuration
@PropertySource("classpath:cybershield.properties")
public class AppConfig {

    public static final String DB_PATH_PROPERTY = "cybershield.db.path";
    public static final String DEFAULT_DB_PATH = "cybershield.db";
    public static final String TELEMETRY_THRESHOLD_PROPERTY = "cybershield.telemetry.threshold";
    public static final String TELEMETRY_COMPARISON_PROPERTY = "cybershield.telemetry.comparison";
    public static final String TELEMETRY_SEVERITY_PROPERTY = "cybershield.telemetry.severity";
    public static final String ANOMALY_WINDOW_SIZE_PROPERTY = "cybershield.anomaly.window-size";
    public static final String ANOMALY_Z_SCORE_THRESHOLD_PROPERTY = "cybershield.anomaly.z-score-threshold";
    public static final String ANOMALY_SEVERITY_PROPERTY = "cybershield.anomaly.severity";
    public static final String ROOT_CAUSE_SKEW_TOLERANCE_SECONDS_PROPERTY =
            "cybershield.root-cause.skew-tolerance-seconds";

    // ---- database ----

    @Bean
    public SqliteConnectionFactory sqliteConnectionFactory(Environment environment) {
        String dbPath = environment.getProperty(DB_PATH_PROPERTY, DEFAULT_DB_PATH);
        return new SqliteConnectionFactory(Path.of(dbPath));
    }

    @Bean(initMethod = "initialize")
    public SchemaInitializer schemaInitializer(SqliteConnectionFactory connections) {
        return new SchemaInitializer(connections);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public StartupCheckService startupCheckService(SqliteConnectionFactory connections) {
        return new StartupCheckService(connections);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public EventRepository eventRepository(SqliteConnectionFactory connections) {
        return new SqliteEventRepository(connections);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public AlertRepository alertRepository(SqliteConnectionFactory connections) {
        return new SqliteAlertRepository(connections);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public IncidentRepository incidentRepository(SqliteConnectionFactory connections, AlertRepository alerts) {
        return new SqliteIncidentRepository(connections, alerts);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public ImportHistoryRepository importHistoryRepository(SqliteConnectionFactory connections) {
        return new SqliteImportHistoryRepository(connections);
    }

    @Bean
    @DependsOn("schemaInitializer")
    public DataResetRepository dataResetRepository(SqliteConnectionFactory connections) {
        return new SqliteDataResetRepository(connections);
    }

    // ---- parsing and detection ----

    @Bean
    public AuthLogParser authLogParser() {
        return new AuthLogParser();
    }

    @Bean
    public EquipmentAlarmCsvParser equipmentAlarmCsvParser() {
        return new EquipmentAlarmCsvParser();
    }

    @Bean
    public LogImportService logImportService() {
        return new LogImportService();
    }

    @Bean
    public BruteForceRule bruteForceRule(Environment environment) {
        return new BruteForceRule(
                environment.getProperty("cybershield.detection.bruteforce.threshold", Integer.class, 5),
                seconds(environment, "cybershield.detection.bruteforce.window.seconds", 10));
    }

    @Bean
    public FailThenSuccessRule failThenSuccessRule(Environment environment) {
        return new FailThenSuccessRule(
                environment.getProperty("cybershield.detection.failthensuccess.failures", Integer.class, 3),
                seconds(environment, "cybershield.detection.failthensuccess.window.seconds", 60));
    }

    @Bean
    public DetectionEngine detectionEngine(BruteForceRule bruteForce, FailThenSuccessRule failThenSuccess,
                                           Environment environment,
                                           ObjectProvider<TelemetryThresholdRule> telemetryRule,
                                           ObjectProvider<RollingZScoreAnomalyDetector> zScoreDetector) {
        List<DetectionRule> rules = new ArrayList<>(List.of(bruteForce, failThenSuccess));
        if (environment.containsProperty(TELEMETRY_THRESHOLD_PROPERTY)) {
            rules.add(telemetryRule.getObject());
        }
        List<AnomalyDetector> detectors = new ArrayList<>();
        if (environment.containsProperty(ANOMALY_WINDOW_SIZE_PROPERTY)) {
            detectors.add(zScoreDetector.getObject());
        }
        return new DefaultDetectionEngine(rules, detectors);
    }

    // ---- equipment track (created only when configured) ----

    @Bean
    @Lazy
    public SqliteRootCauseCandidateStore sqliteRootCauseCandidateStore(SqliteConnectionFactory connections) {
        return new SqliteRootCauseCandidateStore(connections);
    }

    @Bean
    @Lazy
    public TelemetryThresholdRule telemetryThresholdRule(Environment environment) {
        double threshold = environment.getRequiredProperty(TELEMETRY_THRESHOLD_PROPERTY, Double.class);
        TelemetryThresholdRule.Comparison comparison = TelemetryThresholdRule.Comparison.valueOf(
                environment.getRequiredProperty(TELEMETRY_COMPARISON_PROPERTY).trim().toUpperCase(Locale.ROOT));
        Severity severity = Severity.valueOf(
                environment.getRequiredProperty(TELEMETRY_SEVERITY_PROPERTY).trim().toUpperCase(Locale.ROOT));
        return new TelemetryThresholdRule(threshold, comparison, severity);
    }

    @Bean
    @Lazy
    public RollingZScoreAnomalyDetector rollingZScoreAnomalyDetector(Environment environment) {
        int windowSize = environment.getRequiredProperty(ANOMALY_WINDOW_SIZE_PROPERTY, Integer.class);
        double zScoreThreshold = environment.getRequiredProperty(ANOMALY_Z_SCORE_THRESHOLD_PROPERTY, Double.class);
        Severity severity = Severity.valueOf(
                environment.getRequiredProperty(ANOMALY_SEVERITY_PROPERTY).trim().toUpperCase(Locale.ROOT));
        return new RollingZScoreAnomalyDetector(windowSize, zScoreThreshold, severity);
    }

    @Bean
    @Lazy
    public PrecedenceStatistics precedenceStatistics(Environment environment) {
        long skewToleranceSeconds =
                environment.getRequiredProperty(ROOT_CAUSE_SKEW_TOLERANCE_SECONDS_PROPERTY, Long.class);
        return new PrecedenceStatistics(Duration.ofSeconds(skewToleranceSeconds));
    }

    @Bean
    @Lazy
    public RootCauseRanker rootCauseRanker(PrecedenceStatistics statistics) {
        return new PrecedenceRootCauseRanker(statistics);
    }

    // ---- correlation ----

    @Bean
    public CorrelationEngine correlationEngine(Environment environment) {
        return new TimeWindowCorrelationEngine(
                seconds(environment, "cybershield.correlation.window.seconds", 300),
                environment.getProperty("cybershield.correlation.match.source", Boolean.class, true),
                environment.getProperty("cybershield.correlation.match.rule", Boolean.class, false),
                environment.getProperty("cybershield.correlation.match.eventtype", Boolean.class, false));
    }

    // ---- services ----

    @Bean
    public PipelineService pipelineService(LogImportService importService, EventRepository events,
                                           DetectionEngine detection, AlertRepository alerts,
                                           CorrelationEngine correlation, IncidentRepository incidents,
                                           Environment environment, ObjectProvider<RootCauseRanker> rankers,
                                           ObjectProvider<PrecedenceStatistics> statistics,
                                           ImportHistoryRepository history) {
        boolean rankingEnabled = environment.containsProperty(ROOT_CAUSE_SKEW_TOLERANCE_SECONDS_PROPERTY);
        return new PipelineService(importService, events, detection, alerts, correlation, incidents,
                rankingEnabled ? rankers.getObject() : null,
                rankingEnabled ? statistics.getObject() : null,
                history, Clock.systemUTC());
    }

    @Bean
    public MaintenanceService maintenanceService(DataResetRepository reset) {
        return new MaintenanceService(reset);
    }

    @Bean
    public IncidentService incidentService(IncidentRepository incidents) {
        return new DefaultIncidentService(incidents, Clock.systemUTC());
    }

    @Bean
    public AlertService alertService(AlertRepository alerts) {
        return new AlertService(alerts);
    }

    @Bean
    public CsvExporter csvExporter() {
        return new CsvExporter();
    }

    private static Duration seconds(Environment environment, String key, int defaultSeconds) {
        return Duration.ofSeconds(environment.getProperty(key, Integer.class, defaultSeconds));
    }
}
