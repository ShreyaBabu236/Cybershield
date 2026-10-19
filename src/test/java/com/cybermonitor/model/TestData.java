package com.cybermonitor.model;

import java.time.Instant;
import java.util.List;

/** Builds valid sample objects so each test only spells out what it is testing. */
final class TestData {

    static final Instant T0 = Instant.parse("2026-10-18T10:00:00Z");

    private TestData() {
    }

    static LogEvent event() {
        return new LogEvent("e1", T0, "sshd", "LOGIN_FAILED", "admin", null,
                "Failed password for admin", "10:00:01 sshd user=admin LOGIN_FAILED");
    }

    static ThreatAlert alert(List<LogEvent> evidence) {
        return new ThreatAlert("a1", "BRUTE_FORCE", Severity.HIGH, T0, "sshd",
                "5 failures in 10 s (threshold 5)", evidence);
    }

    static Incident incident(List<ThreatAlert> alerts) {
        return new Incident("i1", "Possible account compromise", Severity.HIGH,
                IncidentStatus.OPEN, alerts, List.of(), "");
    }
}
