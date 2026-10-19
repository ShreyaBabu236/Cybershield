package com.cybermonitor.detection;

import com.cybermonitor.model.LogEvent;
import com.cybermonitor.model.ThreatAlert;

import java.util.List;

/**
 * One transparent detection rule, such as "5 failed logins from one source within 10 seconds".
 *
 * <p>Every alert a rule returns must explain itself: the rule name, the observed value,
 * the threshold and the supporting events.
 */
public interface DetectionRule {

    /** Stable name of the rule, used in alerts and reports (for example "BRUTE_FORCE"). */
    String name();

    /**
     * Checks a batch of events.
     *
     * @param events the events to inspect; may be unsorted, so rules must not assume order
     * @return the alerts raised, or an empty list when nothing matched; never null
     */
    List<ThreatAlert> evaluate(List<LogEvent> events);
}
