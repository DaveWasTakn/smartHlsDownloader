package org.davesEnterprise.enums;

public enum CurrentState {
    IDLE("Idle"),
    DOWNLOADING("Downloading..."),
    MERGING("Merging..."),
    FINISHED("Finished!");

    private final String label;

    CurrentState(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}