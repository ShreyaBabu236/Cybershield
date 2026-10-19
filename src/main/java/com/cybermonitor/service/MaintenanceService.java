package com.cybermonitor.service;

import com.cybermonitor.repository.DataResetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/** Housekeeping actions for the UI, such as clearing all stored data. */
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final DataResetRepository reset;

    public MaintenanceService(DataResetRepository reset) {
        this.reset = Objects.requireNonNull(reset, "reset must not be null");
    }

    /** Deletes all events, alerts, incidents, notes and import history. Cannot be undone. */
    public void clearAllData() {
        reset.deleteAll();
        log.info("All stored data was cleared");
    }
}
