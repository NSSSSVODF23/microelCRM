package com.microel.trackerbackend.storage.entities.comments;

public enum AttachmentType {
    PHOTO("PHOTO"), VIDEO("VIDEO"), DOCUMENT("DOCUMENT"), AUDIO("AUDIO"), FILE("FILE");

    private final String type;

    AttachmentType(String type) {
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
