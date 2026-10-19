package com.cybermonitor.model;

/**
 * One possible root cause of an incident, with the reasoning behind its rank.
 *
 * @param eventType   the kind of event suggested as the root cause (for example "PUMP_FAULT"); never blank
 * @param source      the source that produced it; never blank
 * @param score       probability-like score from 0.0 to 1.0: the average chance that this event type
 *                    comes before the other event types in the incident (higher = more likely the cause)
 * @param explanation plain-language evidence, for example the supporting event pairs; never blank
 */
public record RootCauseCandidate(
        String eventType,
        String source,
        double score,
        String explanation) {

    public RootCauseCandidate {
        Checks.requireText(eventType, "eventType");
        Checks.requireText(source, "source");
        if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0 but was " + score);
        }
        Checks.requireText(explanation, "explanation");
    }
}
