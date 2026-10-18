package com.cybermonitor.exception;

/**
 * Base unchecked exception for CyberShield failures that the caller cannot
 * reasonably recover from (for example, the database file cannot be opened).
 */
public class CyberShieldException extends RuntimeException {

    public CyberShieldException(String message) {
        super(message);
    }

    public CyberShieldException(String message, Throwable cause) {
        super(message, cause);
    }
}
