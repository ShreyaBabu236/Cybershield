package com.cybermonitor.model;

import java.util.List;
import java.util.Objects;

/**
 * A group of related alerts that probably come from one underlying problem.
 *
 * <p>Incidents are immutable. To change the status, notes or root causes, use the
 * {@code with...} methods, which return an updated copy.
 *
 * @param id         unique id of the incident; never blank
 * @param title      short summary (for example "Possible account compromise"); never blank
 * @param severity   the severity of the incident, normally the highest severity of its alerts; never null
 * @param status     investigation status; never null
 * @param alerts     the grouped alerts; stored as an unmodifiable copy (null becomes empty)
 * @param rootCauses ranked root-cause candidates, best first; stored as an unmodifiable copy (null becomes empty)
 * @param notes      free-text investigation notes; null is stored as an empty string
 */
public record Incident(
        String id,
        String title,
        Severity severity,
        IncidentStatus status,
        List<ThreatAlert> alerts,
        List<RootCauseCandidate> rootCauses,
        String notes) {

    public Incident {
        Checks.requireText(id, "id");
        Checks.requireText(title, "title");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        rootCauses = rootCauses == null ? List.of() : List.copyOf(rootCauses);
        if (notes == null) {
            notes = "";
        }
    }

    public Incident withStatus(IncidentStatus newStatus) {
        return new Incident(id, title, severity, newStatus, alerts, rootCauses, notes);
    }

    public Incident withNotes(String newNotes) {
        return new Incident(id, title, severity, status, alerts, rootCauses, newNotes);
    }

    public Incident withRootCauses(List<RootCauseCandidate> newRootCauses) {
        return new Incident(id, title, severity, status, alerts, newRootCauses, notes);
    }
}
