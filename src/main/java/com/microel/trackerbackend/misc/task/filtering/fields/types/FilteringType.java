package com.microel.trackerbackend.misc.task.filtering.fields.types;

public enum FilteringType {
    TEXT("TEXT"),
    LOGIN("LOGIN"),
    ADDRESS("ADDRESS"),
    PHONE("PHONE"),
    AD_SOURCE("AD_SOURCE"),
    CONNECTION_TYPE("CONNECTION_TYPE"),
    CONNECTION_SERVICE("CONNECTION_SERVICE");

    private final String value;
    FilteringType(String value) {
        this.value = value;
    }
    public String getValue() {
        return this.value;
    }
}
