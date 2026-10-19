package com.cybermonitor.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogEventTest {

    @Test
    void keepsAllFieldsIncludingTheRawLine() {
        LogEvent event = new LogEvent("e1", TestData.T0, "Pump-1", "PRESSURE", null, 4.2,
                "pressure reading", "2026-10-18T10:00:00Z,Pump-1,PRESSURE,4.2");

        assertEquals("Pump-1", event.source());
        assertEquals(4.2, event.value());
        assertNull(event.user());
        assertEquals("2026-10-18T10:00:00Z,Pump-1,PRESSURE,4.2", event.rawLine());
    }

    @Test
    void nullMessageBecomesEmptyString() {
        LogEvent event = new LogEvent("e1", TestData.T0, "sshd", "LOGIN_OK", "admin", null, null, "raw");

        assertEquals("", event.message());
    }

    @Test
    void rejectsBlankSourceAndEventType() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogEvent("e1", TestData.T0, "  ", "LOGIN_OK", null, null, "", "raw"));
        assertThrows(IllegalArgumentException.class,
                () -> new LogEvent("e1", TestData.T0, "sshd", "", null, null, "", "raw"));
    }

    @Test
    void rejectsNullIdTimestampAndRawLine() {
        assertThrows(NullPointerException.class,
                () -> new LogEvent(null, TestData.T0, "sshd", "LOGIN_OK", null, null, "", "raw"));
        assertThrows(NullPointerException.class,
                () -> new LogEvent("e1", (Instant) null, "sshd", "LOGIN_OK", null, null, "", "raw"));
        assertThrows(NullPointerException.class,
                () -> new LogEvent("e1", TestData.T0, "sshd", "LOGIN_OK", null, null, "", null));
    }
}
