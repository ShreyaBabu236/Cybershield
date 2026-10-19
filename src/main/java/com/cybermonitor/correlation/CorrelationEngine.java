package com.cybermonitor.correlation;

import com.cybermonitor.model.Incident;
import com.cybermonitor.model.ThreatAlert;

import java.util.List;

/**
 * Groups related alerts into incidents, so that one underlying problem produces one incident
 * instead of many separate alerts.
 *
 * <p>Alerts are related when they share a source or event type and fall within a configurable
 * time window. The window and other settings come from Spring configuration, not from constants.
 */
public interface CorrelationEngine {

    /**
     * @param alerts the alerts to group; may be unsorted
     * @return the incidents, each with status OPEN and its alerts attached; never null.
     *         Every input alert appears in exactly one incident.
     */
    List<Incident> correlate(List<ThreatAlert> alerts);
}
