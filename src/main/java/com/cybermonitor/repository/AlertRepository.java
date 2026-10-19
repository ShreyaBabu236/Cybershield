package com.cybermonitor.repository;

import com.cybermonitor.model.ThreatAlert;

import java.util.List;
import java.util.Optional;

/**
 * Storage for alerts, including the ids of their supporting evidence events.
 *
 * <p>Implementations must use prepared statements and save in one transaction.
 */
public interface AlertRepository {

    /** Saves all alerts in one transaction; saving an alert with an existing id replaces it. */
    void saveAll(List<ThreatAlert> alerts);

    /** @return all stored alerts, with their evidence, never null */
    List<ThreatAlert> findAll();

    /** @return the alert with this id, or empty if there is none */
    Optional<ThreatAlert> findById(String alertId);
}
