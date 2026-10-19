package com.cybermonitor.correlation;

import com.cybermonitor.model.Incident;
import com.cybermonitor.model.RootCauseCandidate;

import java.util.List;

/**
 * Suggests which event most likely started an incident.
 *
 * <p>The planned implementation scores each event type by how often it precedes the other
 * event types in historical incidents, treating pairs closer than the configured clock-skew
 * tolerance as undecided. Training the ranker is a detail of the implementation and is not
 * part of this interface.
 */
public interface RootCauseRanker {

    /**
     * @param incident the incident whose alerts' evidence events are inspected
     * @return candidates ordered from most to least likely root cause (best first); may be empty
     *         when the incident has too little evidence; never null
     */
    List<RootCauseCandidate> rank(Incident incident);
}
