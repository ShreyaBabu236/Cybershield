package com.cybermonitor.model;

import java.time.Instant;
import java.util.Objects;

/**
 * One normalized event, produced by an {@code EventParser} from one line of a log or CSV file.
 *
 * @param id        unique id of the event (for example a UUID); never blank
 * @param timestamp when the event happened; never null
 * @param source    which machine, tool, component or service produced it (for example "sshd" or "Pump-1"); never blank
 * @param eventType what kind of event it is (for example "LOGIN_FAILED" or "THRESHOLD_HIGH"); never blank
 * @param user      user name involved, or null when the event has no user
 * @param value     numeric reading for telemetry events (for example a pressure), or null when there is none
 * @param message   human-readable text; null is stored as an empty string
 * @param rawLine   the original line exactly as read, so nothing is lost by normalization; never null
 */
public record LogEvent(
        String id,
        Instant timestamp,
        String source,
        String eventType,
        String user,
        Double value,
        String message,
        String rawLine) {

    public LogEvent {
        Checks.requireText(id, "id");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Checks.requireText(source, "source");
        Checks.requireText(eventType, "eventType");
        Objects.requireNonNull(rawLine, "rawLine must not be null");
        if (message == null) {
            message = "";
        }
    }
}
