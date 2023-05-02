package com.microel.trackerbackend.storage.entities.team.notification;

public enum NotificationType {
    TASK_CREATED("TASK_CREATED"),
    TASK_EDITED("TASK_EDITED"),
    TASK_CLOSED("TASK_CLOSED"),
    TASK_DELETED("TASK_DELETED"),
    TASK_PROCESSED("TASK_PROCESSED"),
    TASK_REOPENED("TASK_REOPENED"),
    TASK_STAGE_CHANGED("TASK_STAGE_CHANGED"),
    YOU_RESPONSIBLE("YOU_RESPONSIBLE"),
    YOU_OBSERVER("YOU_OBSERVER"),
    NEW_COMMENT("NEW_COMMENT"),
    ;

    private String value;
    NotificationType(String value) {
        this.value = value;
    }
}
