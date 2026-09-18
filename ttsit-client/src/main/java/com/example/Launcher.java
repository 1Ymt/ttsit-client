package com.example;

import javafx.application.Application;

/**
 * Entry point for running the GUI from the IDE or a plain classpath.
 *
 * JavaFX refuses to start when the main class itself extends Application and
 * the JavaFX jars are on the classpath rather than the module path
 * ("JavaFX runtime components are missing"). Starting from a class that does
 * not extend Application avoids that check.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(GuiApp.class, args);
    }
}
