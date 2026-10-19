package com.cybermonitor.repository;

import com.cybermonitor.model.LogEvent;

import java.util.List;

/**
 * Storage for normalized events.
 *
 * <p>Implementations (in {@code repository.sqlite}) must use prepared statements and run
 * {@link #saveAll(List)} in one transaction.
 */
public interface EventRepository {

    /** Saves all events in one transaction; saving an event with an existing id replaces it. */
    void saveAll(List<LogEvent> events);

    /** @return all stored events, never null */
    List<LogEvent> findAll();

    /** @return the number of stored events */
    long count();
}
