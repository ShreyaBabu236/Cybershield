package com.cybermonitor.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A warning raised by a detection rule or the anomaly detector, with the reasons for it.
 *
 * @param id          unique id of the alert; never blank
 * @param ruleName    name of the rule or detector that raised it (for example "BRUTE_FORCE"); never blank
 * @param severity    how serious it is; never null
 * @param timestamp   time of the alert, normally the time of the last supporting event; never null
 * @param source      the source the alert is about; never blank
 * @param explanation plain-language reason, including the observed value and threshold; never blank
 * @param evidence    the events that support the alert; stored as an unmodifiable copy (null becomes empty)
 */
public record ThreatAlert(
        String id,
        String ruleName,
        Severity severity,
        Instant timestamp,
        String source,
        String explanation,
        List<LogEvent> evidence) {

    public ThreatAlert {
        Checks.requireText(id, "id");
        Checks.requireText(ruleName, "ruleName");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Checks.requireText(source, "source");
        Checks.requireText(explanation, "explanation");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
