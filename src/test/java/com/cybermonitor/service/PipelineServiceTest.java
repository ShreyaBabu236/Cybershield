package com.cybermonitor.service;

import com.cybermonitor.correlation.RootCauseRanker;
import com.cybermonitor.correlation.TimeWindowCorrelationEngine;
import com.cybermonitor.detection.BruteForceRule;
import com.cybermonitor.detection.DefaultDetectionEngine;
import com.cybermonitor.detection.FailThenSuccessRule;
import com.cybermonitor.model.Incident;
import com.cybermonitor.model.RootCauseCandidate;
import com.cybermonitor.parser.AuthLogParser;
import com.cybermonitor.testutil.InMemoryRepositories;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineServiceTest {

    private final InMemoryRepositories.Events events = new InMemoryRepositories.Events();
    private final InMemoryRepositories.Alerts alerts = new InMemoryRepositories.Alerts();
    private final InMemoryRepositories.Incidents incidents = new InMemoryRepositories.Incidents();
    private final InMemoryRepositories.History history = new InMemoryRepositories.History();

    private PipelineService pipeline(RootCauseRanker ranker) {
        return new PipelineService(new LogImportService(), events,
                new DefaultDetectionEngine(List.of(new BruteForceRule(5, Duration.ofSeconds(10)),
                        new FailThenSuccessRule(3, Duration.ofSeconds(60))), List.of()),
                alerts, new TimeWindowCorrelationEngine(Duration.ofSeconds(300), true, false, false),
                incidents, ranker, history, Clock.systemUTC());
    }

    @Test
    void theSampleFileGoesThroughTheWholePipelineAsDocumented() {
        PipelineResult result = pipeline(null).importFile(Path.of("data/sample/auth_sample.log"), new AuthLogParser());

        assertEquals(22, result.imported().importedLines());
        assertEquals(2, result.imported().malformedLines());
        assertEquals(4, result.alerts().size());
        assertEquals(3, result.incidents().size());
        assertEquals(22, events.count());
        assertEquals(4, alerts.stored.size());
        assertEquals(3, incidents.stored.size());
    }

    @Test
    void theBruteForceAndCompromiseAlertsFromOneAddressShareAnIncident() {
        PipelineResult result = pipeline(null).importFile(Path.of("data/sample/auth_sample.log"), new AuthLogParser());

        Incident merged = result.incidents().stream()
                .filter(i -> i.alerts().size() == 2).findFirst().orElseThrow();

        assertEquals("BRUTE_FORCE + FAIL_THEN_SUCCESS on 203.0.113.5 (2 alerts)", merged.title());
    }

    @Test
    void anOptionalRankerFillsInRootCausesBeforeSaving() {
        RootCauseRanker ranker = incident -> List.of(new RootCauseCandidate("LOGIN_FAILED", "x", 0.8, "first"));

        PipelineResult result = pipeline(ranker).importFile(Path.of("data/sample/auth_sample.log"), new AuthLogParser());

        assertTrue(result.incidents().stream().allMatch(i -> i.rootCauses().size() == 1));
        assertTrue(incidents.stored.values().stream().allMatch(i -> i.rootCauses().size() == 1));
    }

    @Test
    void aRankerThatFailsDoesNotStopTheImport() {
        RootCauseRanker broken = incident -> {
            throw new IllegalStateException("boom");
        };

        PipelineResult result = pipeline(broken).importFile(Path.of("data/sample/auth_sample.log"), new AuthLogParser());

        assertEquals(3, result.incidents().size());
        assertTrue(result.incidents().stream().allMatch(i -> i.rootCauses().isEmpty()));
    }

    @Test
    void aFileWithNoValidEventsGivesAnEmptyResult() {
        ImportResult empty = new LogImportService().importLines("x.log", List.of("garbage"), new AuthLogParser());

        PipelineResult result = pipeline(null).process(empty);

        assertTrue(result.alerts().isEmpty());
        assertTrue(result.incidents().isEmpty());
        assertEquals(0, events.count());
    }

    @Test
    void importingTheSameFileTwiceSkipsTheSecondTime() {
        PipelineService pipeline = pipeline(null);
        Path sample = Path.of("data/sample/auth_sample.log");

        PipelineResult first = pipeline.importFile(sample, new AuthLogParser());
        PipelineResult second = pipeline.importFile(sample, new AuthLogParser());

        assertFalse(first.alreadyImported());
        assertTrue(second.alreadyImported());
        assertTrue(second.alerts().isEmpty());
        assertEquals("auth_sample.log: already imported, skipped", second.summary());
        assertEquals(22, events.count());
        assertEquals(4, alerts.stored.size());
        assertEquals(3, incidents.stored.size());
    }

    @Test
    void aChangedFileIsImportedAgain(@TempDir Path folder) throws Exception {
        PipelineService pipeline = pipeline(null);
        Path file = folder.resolve("a.log");
        Files.writeString(file, "2026-01-01 10:00:01 sshd user=a src=1.1.1.1 LOGIN_OK\n");
        pipeline.importFile(file, new AuthLogParser());

        Files.writeString(file, "2026-01-01 10:00:01 sshd user=a src=1.1.1.1 LOGIN_OK\n"
                + "2026-01-01 10:00:02 sshd user=a src=1.1.1.1 LOGIN_OK\n");
        PipelineResult again = pipeline.importFile(file, new AuthLogParser());

        assertFalse(again.alreadyImported());
        assertEquals(2, again.imported().importedLines());
    }

    @Test
    void aFailedImportIsNotRecordedSoItCanBeRetried(@TempDir Path folder) {
        PipelineService pipeline = pipeline(null);

        try {
            pipeline.importFile(folder.resolve("missing.log"), new AuthLogParser());
        } catch (RuntimeException expected) {
            // the file does not exist
        }

        assertTrue(history.fingerprints.isEmpty());
    }

    @Test
    void theSameContentWithAnotherFormatHasAnotherFingerprint(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("a.log");
        Files.writeString(file, "x\n");
        com.cybermonitor.parser.EventParser other = new AuthLogParser() {
            @Override
            public String formatName() {
                return "Other format";
            }
        };

        LogImportService service = new LogImportService();

        assertFalse(service.fingerprint(file, new AuthLogParser()).equals(service.fingerprint(file, other)));
        assertEquals(service.fingerprint(file, new AuthLogParser()), service.fingerprint(file, new AuthLogParser()));
    }
}
