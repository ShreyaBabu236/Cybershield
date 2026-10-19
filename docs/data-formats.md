# Data formats

All data is synthetic. Every format is one event per line; the parser keeps the original line in `LogEvent.rawLine`.

Lines that are blank or start with `#` are ignored (not counted as events or errors). Any other line that does not match the format is **malformed**: it is skipped, counted, and the first 10 are shown after the import.

## 1. Auth log (`.log`, `.txt`) — parser `AuthLogParser`

```text
yyyy-MM-dd HH:mm:ss <service> user=<name> src=<address> <EVENT_TYPE> [free text]
```

Example:

```text
2026-01-01 10:05:00 sshd user=root src=203.0.113.5 LOGIN_FAILED Invalid password
2026-01-01 10:05:27 sshd user=root src=203.0.113.5 LOGIN_OK Login successful
```

| Part | Meaning | Becomes in `LogEvent` |
| --- | --- | --- |
| `yyyy-MM-dd HH:mm:ss` | Time in **UTC**, 1 s resolution | `timestamp` |
| `<service>` | Program that wrote the line, for example `sshd` | start of `message` |
| `user=<name>` | Account used | `user` |
| `src=<address>` | Where the attempt came from | `source` (rules group by it) |
| `<EVENT_TYPE>` | `LOGIN_FAILED` or `LOGIN_OK` (upper case) | `eventType` |
| free text | Optional | rest of `message` (`sshd: Invalid password`) |

Rules:

- Fields are separated by one or more spaces and must appear in this order.
- A line longer than 2000 characters is malformed.
- Impossible dates (`2026-13-45`) and unknown event types are malformed.
- `value` is always empty for this format.

A sample file is in `data/sample/auth_sample.log`. With the default settings it gives 22 events, 2 malformed lines, 4 alerts and 3 incidents.

## 2. Equipment-alarm CSV (`.csv`) — parser `EquipmentAlarmCsvParser`

Implemented by `EquipmentAlarmCsvParser`. Each non-comment event line has exactly five columns in this order:

```text
timestamp,tool,component,code,severity
```

- `timestamp`: ISO-8601 instant, for example `2026-10-18T10:00:00Z`.
- `tool`: non-empty tool/source name; normalized to `LogEvent.source`.
- `component`: non-empty equipment component; retained in the normalized event message.
- `code`: non-empty alarm/event code; normalized to `LogEvent.eventType`.
- `severity`: non-empty text; retained in the normalized event message.

Blank lines, lines whose first non-whitespace character is `#`, and the case-insensitive header are ignorable. Quoted CSV fields and doubled quotes inside quoted fields are supported. An event row with any blank required field, an invalid timestamp, malformed quoting, or a field count other than five is malformed. The parser preserves the original row as `LogEvent.rawLine`; `LogEvent.value` and `user` are null for this format.

Example (the checked-in equipment sample uses these fields):

```csv
# Synthetic equipment alarm sample
timestamp,tool,component,code,severity
2026-10-18T10:00:00Z,Pump-1,Motor,PUMP_FAULT,CRITICAL
2026-10-18T10:00:15Z,Pump-1,Pressure-Sensor,PRESSURE_HIGH,HIGH
2026-10-18T10:00:30Z,Wafer-Handler-1,Arm,HANDLER_ALARM,HIGH
2026-10-18T10:00:45Z,Recipe-Controller-1,Recipe,RECIPE_ABORT,MEDIUM
```

The sample is under [`data/sample/equipment-alarms.csv`](../data/sample/equipment-alarms.csv). It can be imported in the app by choosing **Equipment alarm CSV** (4 events, no alerts: the security rules only match login events).

## 3. Telemetry CSV

The PRD mentions telemetry CSV but the current source tree does not contain a telemetry parser or agreed column schema. No telemetry CSV sample is supplied as import-ready data. Numeric `LogEvent.value` values can be used directly with the threshold and rolling z-score components in code.

## Settings

Detection and correlation settings are in `src/main/resources/cybershield.properties`. Any of them can be overridden when starting the application, for example `-Dcybershield.detection.bruteforce.threshold=8`.

| Key | Default | Meaning |
| --- | --- | --- |
| `cybershield.detection.bruteforce.threshold` | 5 | Failed logins from one source that raise an alert |
| `cybershield.detection.bruteforce.window.seconds` | 10 | Time window for that count |
| `cybershield.detection.failthensuccess.failures` | 3 | Failures before a success that raise an alert |
| `cybershield.detection.failthensuccess.window.seconds` | 60 | Time window for that sequence |
| `cybershield.correlation.window.seconds` | 300 | Alerts closer than this may form one incident |
| `cybershield.correlation.match.source` | true | Alerts from the same source are related |
| `cybershield.correlation.match.rule` | false | Alerts from the same rule are related |
| `cybershield.correlation.match.eventtype` | false | Alerts whose evidence shares an event type are related |
| `cybershield.db.path` | `cybershield.db` | SQLite file location |
| `cybershield.telemetry.threshold`, `.comparison`, `.severity` | not set | Optional telemetry threshold rule; all three must be set to enable it (no built-in default) |
| `cybershield.anomaly.window-size`, `.z-score-threshold`, `.severity` | not set | Optional rolling z-score detector; all three must be set to enable it |
| `cybershield.root-cause.skew-tolerance-seconds` | not set | Optional root-cause ranker; set to enable it (the ranker returns no candidates until trained) |
