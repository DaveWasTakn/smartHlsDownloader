package org.davesEnterprise.enums;

public enum SegmentValidation {
    NONE("None"),
    METADATA("Metadata"),
    DECODE("Metadata + Decode");

    private final String guiName;

    SegmentValidation(String s) {
        this.guiName = s;
    }

    @Override
    public String toString() {
        return this.guiName;
    }
}
