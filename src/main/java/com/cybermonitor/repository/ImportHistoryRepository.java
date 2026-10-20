package com.cybermonitor.repository;

import java.time.Instant;

/** Remembers which files were already imported, so importing the same file twice is detected. */
public interface ImportHistoryRepository {

    /** @return true if a file with this fingerprint was recorded before */
    boolean contains(String fingerprint);

    /** Records a successful import; recording the same fingerprint again changes nothing. */
    void record(String fingerprint, String fileName, Instant importedAt);
}
