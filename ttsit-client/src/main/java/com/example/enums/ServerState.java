package com.example.enums;

/**
 * Lifecycle of the Python TTS server as shown in the GUI.
 *
 * Each state carries the text shown next to the tally lamp and the style class
 * that theme.css uses to light the lamp: hollow when closed, dim while
 * starting or closing, bright with a halo when running, red when the server
 * could not be started or died unexpectedly.
 */
public enum ServerState {
    CLOSED("Server closed", "state-closed"),
    STARTING("Server starting…", "state-starting"),
    RUNNING("Server running", "state-running"),
    CLOSING("Server closing…", "state-closing"),
    FAILED("Server failed", "state-failed");

    private final String label;
    private final String styleClass;

    ServerState(String label, String styleClass) {
        this.label = label;
        this.styleClass = styleClass;
    }

    public String getLabel() {
        return label;
    }

    public String getStyleClass() {
        return styleClass;
    }
}
