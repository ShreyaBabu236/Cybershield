package com.cybermonitor.service;

import com.cybermonitor.exception.CyberShieldException;
import com.cybermonitor.exception.MalformedLineException;
import com.cybermonitor.model.LogEvent;
import com.cybermonitor.parser.EventParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads a file line by line with Java NIO and turns it into events using an {@link EventParser}.
 *
 * <p>Bad lines never stop the import: they are counted and the first few are reported.
 * This class does not touch the database; the pipeline decides what to save.
 */
public class LogImportService {

    static final int MAX_MALFORMED_SAMPLES = 10;

    public ImportResult importFile(Path file, EventParser parser) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(parser, "parser must not be null");
        if (!Files.isRegularFile(file)) {
            throw new CyberShieldException("Not a readable file: " + file);
        }
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return importLines(file.getFileName().toString(), lines, parser);
        } catch (IOException e) {
            throw new CyberShieldException("Cannot read " + file, e);
        }
    }

    /** Same as {@link #importFile} for lines that are already in memory (used by tests and the generator). */
    public ImportResult importLines(String name, List<String> lines, EventParser parser) {
        List<LogEvent> events = new ArrayList<>();
        List<String> samples = new ArrayList<>();
        int ignored = 0;
        int malformed = 0;
        int number = 0;
        for (String line : lines) {
            number++;
            if (parser.isIgnorable(line)) {
                ignored++;
                continue;
            }
            try {
                events.add(parser.parseLine(line));
            } catch (MalformedLineException e) {
                malformed++;
                if (samples.size() < MAX_MALFORMED_SAMPLES) {
                    samples.add("line " + number + ": " + e.getMessage());
                }
            }
        }
        return new ImportResult(name, lines.size(), ignored, malformed, events, samples);
    }

    /**
     * Identifies a file by what it contains and how it is read: SHA-256 of the bytes plus the format name.
     * The same content imported with another parser has a different fingerprint.
     */
    public String fingerprint(Path file, EventParser parser) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(parser.formatName().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(file));
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new CyberShieldException("Cannot read " + file, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
