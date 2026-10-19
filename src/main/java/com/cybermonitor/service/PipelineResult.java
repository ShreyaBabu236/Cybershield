package com.cybermonitor.service;

import com.cybermonitor.model.Incident;
import com.cybermonitor.model.ThreatAlert;

import java.util.List;

/**
 * What one run of the pipeline produced.
 *
 * @param alreadyImported true if the file had been imported before, so nothing was read or saved
 */
public record PipelineResult(ImportResult imported, List<ThreatAlert> alerts, List<Incident> incidents,
                             boolean alreadyImported) {

    public PipelineResult {
        alerts = List.copyOf(alerts);
        incidents = List.copyOf(incidents);
    }

    public PipelineResult(ImportResult imported, List<ThreatAlert> alerts, List<Incident> incidents) {
        this(imported, alerts, incidents, false);
    }

    public String summary() {
        if (alreadyImported) {
            return imported.fileName() + ": already imported, skipped";
        }
        return imported.summary() + " -> " + alerts.size() + " alerts -> " + incidents.size() + " incidents";
    }
}
