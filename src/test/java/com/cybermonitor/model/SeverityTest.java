package com.cybermonitor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeverityTest {

    @Test
    void orderGoesFromLowToCritical() {
        assertTrue(Severity.LOW.compareTo(Severity.MEDIUM) < 0);
        assertTrue(Severity.MEDIUM.compareTo(Severity.HIGH) < 0);
        assertTrue(Severity.HIGH.compareTo(Severity.CRITICAL) < 0);
    }
}
