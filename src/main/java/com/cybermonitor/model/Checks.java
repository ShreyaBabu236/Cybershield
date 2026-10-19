package com.cybermonitor.model;

import java.util.Objects;

/** Small validation helpers shared by the model records (package-private on purpose). */
final class Checks {

    private Checks() {
    }

    /**
     * @throws NullPointerException     if the value is null
     * @throws IllegalArgumentException if the value is empty or only whitespace
     */
    static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
