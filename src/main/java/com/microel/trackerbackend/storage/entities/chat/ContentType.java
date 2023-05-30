package com.microel.trackerbackend.storage.entities.chat;

public enum ContentType {
    VISUAL("VISUAL"),
    AUDIO("AUDIO"),
    FILE("FILE");
    private final String value;
    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
