package com.microel.trackerbackend.storage.entities.templating;

public enum WireframeFieldType {
    BOOLEAN("BOOLEAN"),
    SMALL_TEXT("SMALL_TEXT"),
    LARGE_TEXT("LARGE_TEXT"),
    INTEGER("INTEGER"),
    FLOAT("FLOAT"),
    ADDRESS("ADDRESS"),
    LOGIN("LOGIN"),
    AD_SOURCE("AD_SOURCE"),
    REQUEST_INITIATOR("REQUEST_INITIATOR"),
    IP("IP"),
    EQUIPMENTS("EQUIPMENTS"),
    CONNECTION_SERVICES("CONNECTION_SERVICES"),
    PHONE_ARRAY("PHONE_ARRAY");

    private final String name;

    WireframeFieldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
