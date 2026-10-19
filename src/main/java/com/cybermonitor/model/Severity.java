package com.cybermonitor.model;

/**
 * How serious an alert or incident is.
 *
 * <p>Constants are declared from least to most serious, so {@code compareTo}
 * orders them by severity (LOW &lt; MEDIUM &lt; HIGH &lt; CRITICAL).
 */
public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
