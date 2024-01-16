package com.microel.trackerbackend.storage.entities.comments;

public enum FileType {
    PHOTO("PHOTO"), VIDEO("VIDEO"), DOCUMENT("DOCUMENT"), AUDIO("AUDIO"), FILE("FILE");

    private final String type;

    FileType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
