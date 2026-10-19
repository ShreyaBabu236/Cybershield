package com.cybermonitor.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentTest {

    private final Incident incident = TestData.incident(List.of(TestData.alert(List.of(TestData.event()))));

    @Test
    void withStatusReturnsAnUpdatedCopyAndLeavesTheOriginalUnchanged() {
        Incident updated = incident.withStatus(IncidentStatus.INVESTIGATING);

        assertEquals(IncidentStatus.INVESTIGATING, updated.status());
        assertEquals(IncidentStatus.OPEN, incident.status());
        assertEquals(incident.alerts(), updated.alerts());
    }

    @Test
    void withNotesAndWithRootCausesKeepEverythingElse() {
        RootCauseCandidate cause = new RootCauseCandidate("PUMP_FAULT", "Pump-1", 0.9, "precedes 9 of 10");

        Incident updated = incident.withNotes("Checked the pump logs").withRootCauses(List.of(cause));

        assertEquals("Checked the pump logs", updated.notes());
        assertEquals(List.of(cause), updated.rootCauses());
        assertEquals(incident.title(), updated.title());
        assertEquals(incident.status(), updated.status());
    }

    @Test
    void nullNotesAndNullListsBecomeEmpty() {
        Incident empty = new Incident("i2", "Title", Severity.LOW, IncidentStatus.OPEN, null, null, null);

        assertEquals("", empty.notes());
        assertTrue(empty.alerts().isEmpty());
        assertTrue(empty.rootCauses().isEmpty());
    }

    @Test
    void alertAndRootCauseListsCannotBeModified() {
        assertThrows(UnsupportedOperationException.class, () -> incident.alerts().clear());
        assertThrows(UnsupportedOperationException.class, () -> incident.rootCauses().clear());
    }

    @Test
    void sameAlertObjectsAreKeptNotCloned() {
        ThreatAlert alert = TestData.alert(List.of());

        Incident withAlert = TestData.incident(List.of(alert));

        assertSame(alert, withAlert.alerts().get(0));
    }
}
