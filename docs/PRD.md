# CyberShield Desktop PRD (v2: with root-cause ranking)

Oct 17, 2026 · @vinuba k

## 1. Overview

CyberShield Desktop is a Java 21 desktop tool that turns many noisy log events into a few explained incidents, and measures how much noise it removes. It is built by two students and uses only synthetic data.

**Problem.** Complex systems (IT servers, equipment software, semiconductor tools) emit large volumes of logs and alarms. One fault often triggers dozens of messages, so engineers cannot tell quickly which events belong together.

**Honest framing.** Alert overload and root-cause analysis are well-known problems, already addressed by commercial tools. This project is a small, transparent version. It makes no claim about KLA's internal systems or data.

**Worked example.** Five `LOGIN_FAILED` events from one source in 10 seconds raise a `BRUTE_FORCE` alert. A `LOGIN_OK` from the same source 22 seconds later links to it. Correlation merges both into one incident, "Possible account compromise", listing the rule, the observed value (5 vs threshold 5), the window and the 6 supporting events.

**Enhancement (v2): root-cause ranking.** One fault often triggers many downstream alarms, and clock skew between sources blurs which event came first. Version 2 adds a transparent, precedence-based ranker that suggests the likely root cause of each incident, and measures it against a plain earliest-timestamp baseline. It is a small attempt at a known open sub-problem, not a solution to it.

**Enhancement example.** A pump `FAULT` is logged at 10:00:01.8 by a clock running 0.5 s behind. Then come a pressure `THRESHOLD_HIGH` at 10:00:02.1, a wafer-handler `ALARM` at 10:00:03.0 and a recipe `ABORT` at 10:00:04.2. The incident shows "Likely root cause: pump FAULT (precedes N% of related alarms in the training data)", where N is computed by the program from the generated dataset.

## 2. Goals, non-goals and success metrics

The product succeeds if it runs locally, explains every incident, and reports measured (not invented) noise reduction.

**Goals**

- Import and normalize auth logs, telemetry CSV and synthetic equipment-alarm CSV into one event model.
- Detect brute force, telemetry threshold breaches, fail-then-success sequences and statistical anomalies.
- Correlate alerts into incidents with a human-readable explanation.
- Rank the likely root cause of each incident from learned event precedence, with configurable clock-skew tolerance.
- Persist everything in SQLite and let users investigate in JavaFX.
- Evaluate two comparisons on labeled synthetic data: independent alerts vs correlated incidents, and earliest-timestamp baseline vs precedence ranker.

**Non-goals**

- No web app, REST API, browser UI or cloud service.
- No machine-learning frameworks, no real or proprietary data.
- No claim of real-world accuracy from synthetic results.

**Success metrics** (all produced by running the evaluation, never typed by hand)

| Metric | How it is measured |
| --- | --- |
| Precision, recall, F1 | Alerts vs ground-truth labels in the generated dataset |
| False-positive rate | False alerts / all normal events |
| Alert reduction | 1 - (incidents / raw alerts) |
| Throughput | Events processed per second on a documented dataset size |
| Root-cause top-1 and top-3 accuracy | Share of test cascades where the injected root cause is ranked first, or in the top three |
| Skew robustness | Top-1 and top-3 accuracy at 0 s, 1 s and 3 s injected clock skew, baseline vs ranker |

## 3. Tech stack, architecture and shared contracts

The whole app is Java 21 and desktop-only; the UI layer never touches JDBC and the engines never import JavaFX.

| Technology | Purpose |
| --- | --- |
| Java 21 | All application logic |
| JavaFX | Desktop UI, tables and charts |
| Spring Core (`spring-context`) | Dependency injection and config |
| Java NIO | Streaming file import |
| SQLite + JDBC | Local storage (prepared statements, transactions) |
| Maven | Build and dependencies |
| JUnit 5 | Unit and integration tests |
| SLF4J + Logback | Logging |
| Git + GitHub | Branches, pull requests, review |

**Data flow**

1. User picks a `.log`, `.txt` or `.csv` file in the JavaFX import screen.
2. `LogImportService` streams it with NIO and hands each line to an `EventParser`.
3. Parsers output normalized `LogEvent` objects; malformed lines are counted and skipped.
4. `DetectionEngine` runs all `DetectionRule`s and the `AnomalyDetector`, producing `ThreatAlert`s with explanations.
5. `CorrelationEngine` groups alerts by time window and source into `Incident`s.
6. `RootCauseRanker` scores the events in each incident using precedence statistics and attaches ranked `RootCauseCandidate`s.
7. Repositories save events, alerts, incidents and root-cause candidates to SQLite.
8. JavaFX shows the dashboard, alert table, incident detail with root-cause panel, filters and CSV export.

**Shared contracts (agreed first, merged to `main` before feature work)**

- `Severity` enum: LOW, MEDIUM, HIGH, CRITICAL.
- `LogEvent` record: id, timestamp, source, eventType, user, value, message, rawLine.
- `ThreatAlert` record: id, ruleName, severity, timestamp, source, explanation, evidence (list of `LogEvent`).
- `RootCauseCandidate` record: eventType, source, score, explanation.
- `Incident` record: id, title, severity, status, alerts, rootCauses (list of `RootCauseCandidate`), notes.
- Interfaces: `EventParser`, `DetectionRule` (`List<ThreatAlert> evaluate(List<LogEvent>)`), `RootCauseRanker` (`List<RootCauseCandidate> rank(Incident incident)`), `EventRepository`, `AlertRepository`, `IncidentRepository`.

Base package: `com.cybermonitor` with `model`, `parser`, `detection`, `correlation`, `anomaly`, `service`, `repository.sqlite`, `evaluation`, `reporting`, `ui`, `config`, `exception`.

**Root-cause method (v2)**

1. Train: over historical incidents in a training dataset, count for each ordered pair of event types (A, B) how often A occurs before B in the same incident.
2. Skew tolerance: if A and B are closer than the configured skew `s` seconds, the pair counts as undecided (0.5 each way), because the order cannot be trusted.
3. Score: for each event type A in a new incident, score(A) is the average of P(A before B) over the other event types B in that incident.
4. Rank: sort by score; break ties by earliest timestamp. Show the top three with the pairs that support each score.
5. Baseline for comparison: rank by earliest timestamp only.

Training and test datasets use different random seeds, so the ranker is never scored on data it learned from.

## 4. Functional requirements

P0 items are the minimum demo, P1 completes the project, P2 is cut first if time runs out. M1 and M2 are Member 1 and Member 2.

| ID | Requirement | Priority | Owner |
| --- | --- | --- | --- |
| FR1 | Import `.log`, `.txt` and documented CSV files; skip and count malformed lines | P0 | M1 parse, M2 UI |
| FR2 | Normalize all formats into `LogEvent`, keeping the raw line | P0 | M1 |
| FR3 | Rule: repeated authentication failures within a time window | P0 | M1 |
| FR4 | Rules: telemetry threshold breach; fail-then-success sequence | P1 | M1 |
| FR5 | Correlate alerts by time window and source into incidents | P0 | M1 |
| FR6 | Explainable alert: rule, observed value, threshold, severity, evidence events | P0 | M1 data, M2 display |
| FR7 | Z-score anomaly detection on telemetry (rolling mean and std-dev) | P1 | M1 |
| FR8 | SQLite persistence of events, alerts, incidents, root causes, notes; reload on restart | P0 | M2 |
| FR9 | Incident details, notes and status (Open, Investigating, Resolved) | P0 | M2 |
| FR10 | Dashboard: event count, severity chart, raw alerts vs incidents | P1 | M2 |
| FR11 | Filters: severity, time range, event type, source, incident status | P1 | M2 |
| FR12 | CSV export of alerts, incidents and evaluation results | P1 | M2 |
| FR13 | Labeled synthetic dataset generator and evaluation report | P0 | M1 |
| FR14 | Unit and integration tests; README with run steps | P0 | Both |
| FR15 | Packaged runnable build, screenshots, demo script | P2 | M2 |
| FR16 | Documented synthetic equipment-alarm CSV format and parser (timestamp, tool, component, code, severity) | P0 | M1 |
| FR17 | Precedence statistics with configurable clock-skew tolerance | P0 | M1 |
| FR18 | Root-cause ranking per incident with top-3 list and explanation | P0 | M1 engine, M2 display |
| FR19 | Cascade generator: injected root cause, downstream alarms, skew levels, separate train and test seeds | P0 | M1 |
| FR20 | Root-cause evaluation: earliest-timestamp baseline vs ranker, top-1 and top-3 at several skew levels | P0 | M1 |

## 5. Two-member work split

Member 1 owns detection, correlation and root-cause analysis; Member 2 owns the desktop app and persistence. Work runs in seven phases (section 7); each ends with a joint checkpoint, and the next phase starts only when it passes.

### Member 1: Detection and Analysis

| Phase | Task | Input | Output | Depends on | Done when |
| --- | --- | --- | --- | --- | --- |
| 0 Contracts | Shared models and interfaces (with M2) | Section 3 contracts | Merged `model` package | None | Both compile on `main` |
| 1 Import | Auth-log parser and equipment-alarm CSV parser | Sample files | `List<LogEvent>`, malformed count | Models | Samples parse; bad lines do not crash |
| 2 Detection | Rules: brute force, fail-then-success, telemetry threshold | `List<LogEvent>` | `List<ThreatAlert>` with explanations | Parsers | Each rule fires on its sample |
| 3 Correlation | `CorrelationEngine` | Alerts | `List<Incident>` | Rules | Worked example gives 1 incident |
| 3 Correlation | Dataset generator v1 and JUnit tests for parsers, rules, correlation | Seeded generator | Labeled dataset; green `mvn test` | Correlation | Tests pass |
| 4 Root cause | Precedence statistics with skew tolerance | Training incidents | Pair counts and probabilities | Correlation | Unit tests cover boundary and undecided pairs |
| 4 Root cause | `RootCauseRanker` with explanation text | Incident, statistics | Ranked `RootCauseCandidate` list on each `Incident` | Statistics | Pump example ranks the pump first |
| 4 Root cause | Cascade generator: injected root cause, skew levels, train and test seeds | Seeds, skew values | Labeled cascades | Ranker interface | Two seeds give different data |
| 5 Evaluation | Evaluation: P/R/F1, FPR, alert reduction, throughput, top-1 and top-3 | Test dataset | Metrics report from a real run | Ranker, generator | Numbers printed and saved to CSV |
| 5 Evaluation | Z-score `AnomalyDetector` and tests | Telemetry events | Anomaly alerts | Parser | Injected spikes flagged |
| 6 Finalization | Method, results and limitations write-up | Evaluation output | README sections | Evaluation | Every number traces to a run |

### Member 2: Desktop App and Persistence

| Phase | Task | Input | Output | Depends on | Done when |
| --- | --- | --- | --- | --- | --- |
| 0 Contracts | Shared models and interfaces (with M1) | Section 3 contracts | Merged `model` package | None | Both compile on `main` |
| 1 Import | `pom.xml`, Spring `AppConfig`, JavaFX window, SQLite connection | Dependency list | Running skeleton | None | Window opens; `SELECT 1` logged |
| 1 Import | Import screen and `LogImportService` (NIO) | File path | Events passed to parser | Parser (stub until M1 merges) | Event count shown after import |
| 2 Detection | `schema.sql` and JDBC repositories | Model records | Event, alert, incident repositories | Models | Save and reload works |
| 2 Detection | Alerts table with explanation | Stored alerts | Alerts view | Repositories, M1 rules | Selecting an alert shows rule, value and evidence |
| 3 Correlation | Incident detail with evidence, notes and status changes | Stored incidents | Investigation UI | Repositories, M1 correlation | Notes and status survive restart |
| 4 Root cause | Schema update for root-cause candidates | Incident model | New table and repository methods | Repositories | Candidates save and reload |
| 4 Root cause | Root-cause panel: ranked top 3 with explanation | `Incident.rootCauses` | Panel in incident detail | M1 ranker | Panel shows candidates and scores |
| 5 Evaluation | Filters: severity, time range, type, source, status | Stored data | Filterable table | Repositories | Filters change the table |
| 5 Evaluation | Dashboard: severity chart, raw alerts vs incidents | Stored data | Charts | Repositories | Counts match the database |
| 5 Evaluation | CSV export of alerts, incidents, evaluation results; persistence integration test | Stored data, metrics | Export files; passing test | Repositories, M1 evaluation | Files open in a spreadsheet |
| 6 Finalization | README, packaging, screenshots, demo script | Whole project | Runnable build and docs | All | Fresh clone runs |

### Shared responsibilities

- Agree on models and method signatures before parallel work starts.
- Review each other's pull requests.
- Integrate on the checkpoints in section 7, not at the end.
- Both must be able to explain the pipeline, tests and evaluation in the demo and in interviews.

## 6. Git workflow

One private GitHub repo, `main` always compiles, and every change reaches `main` through a reviewed pull request.

**Setup (Phase 0)**

1. Member 2 creates the repo with `.gitignore` (`target/`, `.idea/`, `*.db`), invites Member 1, and pushes the empty Maven skeleton.
2. Member 1 pushes `feature/models` with the shared records and interfaces; Member 2 reviews and merges it.

**Branches**

| Branch | Owner | Contents |
| --- | --- | --- |
| `main` | Both | Always builds; protected, no direct pushes |
| `feature/parsers` | M1 | Auth-log and equipment-alarm parsers, malformed-line handling |
| `feature/rules` | M1 | Detection rules |
| `feature/correlation` | M1 | Correlation engine, incidents |
| `feature/precedence` | M1 | Precedence statistics and root-cause ranker |
| `feature/anomaly-eval` | M1 | Cascade generator, evaluation, z-score detector |
| `feature/app-skeleton` | M2 | `pom.xml`, Spring, JavaFX window, SQLite |
| `feature/persistence` | M2 | Schema and repositories |
| `feature/ui-import-alerts` | M2 | Import screen, alerts table, incident detail |
| `feature/rootcause-ui` | M2 | Root-cause panel and schema update |
| `feature/dashboard-export` | M2 | Filters, dashboard, CSV export |

**Rules**

- Commit small and often, with messages like `parser: skip malformed CSV rows`.
- Before opening a PR, run `git pull --rebase origin main` and `mvn clean test`.
- The other member reviews each PR within 10 minutes; a quick read of the diff counts.
- Never commit `cybershield.db`, build output or IDE files.
- Models and interfaces change only through a PR both members approve.

## 7. Phases, checkpoints and priorities

The project has seven phases and no fixed clock times. A phase is finished only when its checkpoint passes on `main` for both members, so problems are caught before the next phase builds on them.

| Phase | Checkpoint | Pass condition |
| --- | --- | --- |
| 0 Contracts | Models and interfaces agreed | Merged on `main`; both members compile |
| 1 Import | Skeleton and parsers work | Window opens, Spring starts, SQLite connects; sample files parse and malformed lines are counted |
| 2 Detection | First alerts on screen | Importing the sample log shows brute-force alerts in the UI and in SQLite after restart |
| 3 Correlation | Incidents investigable | Worked example gives one incident; incident detail shows evidence, notes and status |
| 4 Root cause | Ranker visible | Incident detail shows the top-3 root-cause candidates; unit tests pass for skew boundaries |
| 5 Evaluation | Real metrics | Both comparisons run from a seeded dataset; filters, dashboard and exports work |
| 6 Finalization | Demo ready | Fresh clone builds and runs; README, screenshots and demo script done; each member explains the other's half |

**If scope must shrink, cut in this order**

1. Packaging and screenshots (FR15).
2. Dashboard charts (FR10).
3. Z-score anomaly detector (FR7).
4. Extra filters (FR11).

Never cut correlation, explainable alerts, SQLite persistence, root-cause ranking or the evaluation; they are the core of the project.

## 8. Testing, evaluation, acceptance criteria and risks

The project is done when the checklist below passes on a fresh clone and every reported number comes from a real run.

**Testing**

- Unit tests: parsers (valid, malformed, empty lines), each rule, correlation window boundaries, z-score detector.
- Precedence tests: pair counts, pairs closer than the skew tolerance counted as undecided, stable ranking on ties.
- Edge cases: duplicate events, out-of-order timestamps, events exactly on a window boundary, an incident with a single event.
- Integration test: save and reload alerts, incidents and root-cause candidates through SQLite.

**Evaluation plan**

- Generate seeded synthetic datasets with ground-truth labels (normal events, brute-force bursts, telemetry spikes, alarm cascades with an injected root cause). Use separate seeds for training and test, and record seed and size.
- Comparison 1: independent rule alerts vs correlated incidents. Report precision, recall, F1, false-positive rate, alert reduction and events per second.
- Comparison 2: earliest-timestamp baseline vs precedence ranker. Report top-1 and top-3 accuracy at 0 s, 1 s and 3 s skew, with the number of cascades per setting.
- Limitations to state: the generator defines the causal order the ranker learns, so results show the method works on the assumed structure, not on real equipment. A result where the ranker does not beat the baseline is reported as is.

**Acceptance criteria**

- [ ] `mvn clean test` ends with BUILD SUCCESS.
- [ ] `mvn javafx:run` opens the app on both members' machines.
- [ ] Importing the sample log and the equipment-alarm CSV produces alerts and at least one incident.
- [ ] Selecting an incident shows rule, observed value, severity and supporting events.
- [ ] Incident detail shows the top-3 root-cause candidates with scores and the supporting event pairs.
- [ ] Data, including root-cause candidates, survives an app restart.
- [ ] CSV export of alerts, incidents and evaluation results opens in a spreadsheet.
- [ ] Evaluation prints both comparisons from an actual run, with seeds and sizes recorded.
- [ ] README explains setup, run, test, method and limitations.

**Risks**

| Risk | Fallback |
| --- | --- |
| Models change mid-project and break the other member's code | Change them only through a PR both approve |
| JavaFX or Maven setup problems | Member 2 fixes first; M1 tests logic from JUnit without the UI |
| M2 blocked waiting for parsers | Use a stub parser returning fixed events |
| Ranker is no better than the baseline | Report it honestly and analyze why (ambiguous pairs, low skew); a negative result is still a valid evaluation |
| Ranker scored on data it learned from | Separate training and test seeds, checked in a unit test |
| Evaluation is too slow or too large | Use a smaller dataset and record its size; never invent numbers |
| Merge conflicts | Rebase often and integrate at every checkpoint |
