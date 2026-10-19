package com.cybermonitor.repository;

/** Removes all stored data (events, alerts, incidents, notes and import history) in one transaction. */
public interface DataResetRepository {

    void deleteAll();
}
