package com.cybermonitor.anomaly;

import com.cybermonitor.model.LogEvent;
import com.cybermonitor.model.ThreatAlert;

import java.util.List;

/**
 * Flags telemetry values that are unusual compared with their recent normal range.
 *
 * <p>The planned implementation is a transparent statistical baseline: a rolling mean and
 * standard deviation (z-score). It is not machine learning, and alerts must show the value,
 * the baseline mean, the standard deviation and the z-score.
 */
public interface AnomalyDetector {

    /**
     * @param events events that carry a numeric {@link LogEvent#value()}; events without a value are ignored
     * @return anomaly alerts, or an empty list when nothing is unusual; never null
     */
    List<ThreatAlert> detect(List<LogEvent> events);
}
