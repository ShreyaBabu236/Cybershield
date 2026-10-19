package com.cybermonitor.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatAlertTest {

    @Test
    void evidenceIsACopyThatLaterChangesToTheOriginalListCannotAffect() {
        List<LogEvent> original = new ArrayList<>(List.of(TestData.event()));

        ThreatAlert alert = TestData.alert(original);
        original.clear();

        assertEquals(1, alert.evidence().size());
    }

    @Test
    void evidenceCannotBeModified() {
        ThreatAlert alert = TestData.alert(List.of(TestData.event()));

        assertThrows(UnsupportedOperationException.class, () -> alert.evidence().add(TestData.event()));
    }

    @Test
    void nullEvidenceBecomesEmptyList() {
        ThreatAlert alert = TestData.alert(null);

        assertTrue(alert.evidence().isEmpty());
    }

    @Test
    void rejectsBlankExplanationAndNullSeverity() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThreatAlert("a1", "BRUTE_FORCE", Severity.HIGH, TestData.T0, "sshd", " ", List.of()));
        assertThrows(NullPointerException.class,
                () -> new ThreatAlert("a1", "BRUTE_FORCE", null, TestData.T0, "sshd", "reason", List.of()));
    }
}
