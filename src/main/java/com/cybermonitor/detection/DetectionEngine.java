package com.cybermonitor.detection;

import com.cybermonitor.model.LogEvent;
import com.cybermonitor.model.ThreatAlert;

import java.util.List;

/**
 * Runs all detection rules and the anomaly detector over a batch of events.
 *
 * <p>The engine only collects alerts; grouping them into incidents is the job of the
 * {@code CorrelationEngine}.
 */
public interface DetectionEngine {

    /**
     * @param events the normalized events of one import
     * @return all alerts from all rules and detectors, never null
     */
    List<ThreatAlert> detect(List<LogEvent> events);
}
