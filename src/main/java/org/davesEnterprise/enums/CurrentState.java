package org.davesEnterprise.enums;

public enum CurrentState {
    IDLE("Idle"),
    PARSING_PLAYLIST("Parsing Playlist..."),
    DOWNLOADING("Downloading..."),
    MERGING("Merging..."),
    FINISHED("Finished!"),
    ERROR("ERROR :(");

    private final String label;

    CurrentState(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}