package com.cybermonitor.parser;

import com.cybermonitor.exception.MalformedLineException;
import com.cybermonitor.model.LogEvent;

/**
 * Turns the lines of one file format into {@link LogEvent}s.
 *
 * <p>One parser handles one format (for example the auth log, or the equipment-alarm CSV).
 * The import service reads a file line by line with Java NIO and, for each line:
 * <ol>
 *   <li>skips it if {@link #isIgnorable(String)} is true (not counted),</li>
 *   <li>otherwise calls {@link #parseLine(String)} and counts a {@link MalformedLineException}
 *       as one malformed line.</li>
 * </ol>
 * Implementations must be stateless between lines so they are easy to test.
 */
public interface EventParser {

    /** Short name of the format, shown in the UI (for example "Auth log"). */
    String formatName();

    /** True for lines that carry no event: blank lines, comments and CSV header rows. */
    boolean isIgnorable(String line);

    /**
     * Parses one line.
     *
     * @return the normalized event, with the original line kept in {@link LogEvent#rawLine()}
     * @throws MalformedLineException if the line does not match the format
     */
    LogEvent parseLine(String line) throws MalformedLineException;
}
