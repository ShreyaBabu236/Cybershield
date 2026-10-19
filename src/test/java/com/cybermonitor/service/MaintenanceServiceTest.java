package com.cybermonitor.service;

import com.cybermonitor.model.IncidentStatus;
import com.cybermonitor.model.Severity;
import com.cybermonitor.model.Incident;
import com.cybermonitor.testutil.Fixtures;
import com.cybermonitor.testutil.InMemoryRepositories;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceServiceTest {

    @Test
    void clearAllDataEmptiesEverything() {
        InMemoryRepositories.Events events = new InMemoryRepositories.Events();
        InMemoryRepositories.Alerts alerts = new InMemoryRepositories.Alerts();
        InMemoryRepositories.Incidents incidents = new InMemoryRepositories.Incidents();
        InMemoryRepositories.History history = new InMemoryRepositories.History();
        events.saveAll(List.of(Fixtures.failed("s", "u", 0)));
        alerts.saveAll(List.of(Fixtures.alert("R", "s", 0)));
        incidents.save(new Incident("i1", "t", Severity.LOW, IncidentStatus.OPEN, List.of(), List.of(), ""));
        history.record("abc", "a.log", Fixtures.T0);

        new MaintenanceService(new InMemoryRepositories.Reset(events, alerts, incidents, history)).clearAllData();

        assertEquals(0, events.count());
        assertTrue(alerts.findAll().isEmpty());
        assertTrue(incidents.findAll().isEmpty());
        assertTrue(history.fingerprints.isEmpty());
    }
}
