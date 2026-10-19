package com.cybermonitor.exception;

/**
 * Thrown by an {@code EventParser} when a line cannot be turned into an event.
 *
 * <p>This is an expected situation, not a failure: the import service catches it,
 * counts the line as malformed and continues with the next line. It is a checked
 * exception so that every caller has to decide what to do with a bad line.
 */
public class MalformedLineException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String line;

    public MalformedLineException(String message, String line) {
        super(message);
        this.line = line;
    }

    public MalformedLineException(String message, String line, Throwable cause) {
        super(message, cause);
        this.line = line;
    }

    /** The line that could not be parsed, exactly as read. */
    public String line() {
        return line;
    }
}
