package com.cybermonitor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RootCauseCandidateTest {

    @Test
    void acceptsScoresFromZeroToOneInclusive() {
        assertDoesNotThrow(() -> {
            new RootCauseCandidate("PUMP_FAULT", "Pump-1", 0.0, "reason");
        });
        assertDoesNotThrow(() -> {
            new RootCauseCandidate("PUMP_FAULT", "Pump-1", 1.0, "reason");
        });
    }

    @Test
    void rejectsScoresOutsideZeroToOneAndNaN() {
        assertThrows(IllegalArgumentException.class,
                () -> new RootCauseCandidate("PUMP_FAULT", "Pump-1", -0.01, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> new RootCauseCandidate("PUMP_FAULT", "Pump-1", 1.01, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> new RootCauseCandidate("PUMP_FAULT", "Pump-1", Double.NaN, "reason"));
    }

    @Test
    void rejectsBlankTextFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new RootCauseCandidate(" ", "Pump-1", 0.5, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> new RootCauseCandidate("PUMP_FAULT", "Pump-1", 0.5, ""));
    }
}
