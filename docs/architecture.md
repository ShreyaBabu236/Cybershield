# Architecture and implementation status

CyberShield is organized around shared records and interfaces in `com.cybermonitor`. The intended flow is:

```text
input file -> EventParser -> LogEvent -> DetectionRule / AnomalyDetector
           -> ThreatAlert -> CorrelationEngine -> Incident
           -> RootCauseRanker -> RootCauseCandidate -> persistence and JavaFX
```

The current code implements these independent capabilities:

- `EquipmentAlarmCsvParser` parses one equipment-alarm CSV row into a `LogEvent`.
- `TelemetryThresholdRule` evaluates numeric event values against explicitly configured directional thresholds.
- `RollingZScoreAnomalyDetector` compares each reading with prior numeric readings in its source/event-type series.
- `PrecedenceStatistics` learns event-type pair probabilities from training incidents and applies configured clock-skew tolerance to training pairs.
- `PrecedenceRootCauseRanker` scores incident event types from those probabilities and provides pair evidence in explanations.
- `SyntheticCascadeGenerator` generates clearly marked, seeded root-cause cascades.
- `RootCauseEvaluation` evaluates earliest-timestamp and precedence-based rankings using separate training/test seeds and the PRD's 0/1/3-second injected-skew levels.
- `SqliteRootCauseCandidateStore` persists ordered candidate data by incident ID.
- `DashboardView` renders a supplied `DashboardSnapshot`; it does not query repositories itself.
- `data/sample/equipment-alarms.csv` is checked against the implemented parser in a JUnit test; `data/sample/auth.log` is an illustrative fixture only.

## Runtime and build

`Launcher` starts `CyberShieldApp`; the application creates an `AnnotationConfigApplicationContext`, checks that SQLite answers `SELECT 1`, and displays the main window (Alerts, Incidents and Dashboard tabs). The JavaFX Maven plugin supplies the JavaFX runtime for `mvn javafx:run`. The project does not currently create a self-contained runtime image/native installer.

## Integration gaps

- An import pipeline connects files to incidents: `LogImportService` parses a file with the selected `EventParser`, and `PipelineService` saves the events, runs `DetectionEngine`, saves the alerts, groups them with `CorrelationEngine`, optionally adds ranked root causes when a `RootCauseRanker` is configured, and saves the incidents. Re-importing identical content with the same format is skipped.
- SQLite implementations exist for the event, alert, incident, import-history
and data-reset repositories, plus the root-cause candidate store.
`SchemaInitializer` creates the schema at startup.
- `PrecedenceStatistics` and `RootCauseRanker` are Spring beans, but no historical-incident provider trains the statistics for live application use. `RootCauseEvaluation` trains separate local statistics from synthetic training cascades.
- `DashboardView` is the **Dashboard** tab of the main window. `CyberShieldApp` supplies a `DashboardSnapshot` built from `EventRepository.count()` and `IncidentService.listIncidents()`, reloaded after each import, after Clear data, and when the tab is opened. No `RootCauseEvaluation` report is supplied yet, so that panel says so.
- The standalone candidate store uses `incident_id` as a key, but does not declare a foreign key because no incident table/schema is present.

## Reproducibility and limitations

Evaluation uses synthetic generated cascades and labels. Each cascade has a fixed four-stage event-type sequence and generated intervals. Those assumptions define the training evidence; measured accuracy is not real-world performance. Seeds, dataset sizes, skew levels, and ranker tolerance are included in the evaluation report.
