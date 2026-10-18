package com.cybermonitor;

import javafx.application.Application;

/**
 * Plain main class that starts the JavaFX application.
 *
 * <p>A main class that itself extends {@link Application} often fails with
 * "JavaFX runtime components are missing" when started from an IDE, so both
 * {@code mvn javafx:run} and the IDE run configuration use this class instead.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(CyberShieldApp.class, args);
    }
}
