package com.cybermonitor.service;

import com.cybermonitor.model.Incident;
import com.cybermonitor.model.IncidentStatus;

import java.util.List;
import java.util.Optional;

/**
 * The incident workflow used by the JavaFX UI: browse incidents, change their status and add notes.
 *
 * <p>The UI calls this interface and never touches repositories or JDBC directly.
 */
public interface IncidentService {

    /** @return all stored incidents, never null */
    List<Incident> listIncidents();

    /** @return the incident with this id, or empty if there is none */
    Optional<Incident> findIncident(String incidentId);

    /**
     * Changes the status and saves the incident.
     *
     * @return the updated incident
     * @throws com.cybermonitor.exception.CyberShieldException if no incident has this id
     */
    Incident changeStatus(String incidentId, IncidentStatus newStatus);

    /**
     * Appends a note to the incident's notes and saves it.
     *
     * @return the updated incident
     * @throws com.cybermonitor.exception.CyberShieldException if no incident has this id
     */
    Incident addNote(String incidentId, String note);
}
