package com.cybermonitor.repository;

import com.cybermonitor.model.Incident;

import java.util.List;
import java.util.Optional;

/**
 * Storage for incidents, including their notes, status and root-cause candidates.
 *
 * <p>Implementations must use prepared statements and save in one transaction.
 */
public interface IncidentRepository {

    /** Inserts the incident, or updates it if an incident with the same id already exists. */
    void save(Incident incident);

    /** Saves all incidents in one transaction, with the same insert-or-update rule as {@link #save}. */
    void saveAll(List<Incident> incidents);

    /** @return all stored incidents, never null */
    List<Incident> findAll();

    /** @return the incident with this id, or empty if there is none */
    Optional<Incident> findById(String incidentId);
}
