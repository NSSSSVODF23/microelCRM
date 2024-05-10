package com.microel.trackerbackend.storage.entities.team.notification;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.storage.entities.team.util.PhyPhoneInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum NotificationType {
    TASK_CREATED("TASK_CREATED"),
    TASK_EDITED("TASK_EDITED"),
    TASK_CLOSED("TASK_CLOSED"),
    TASK_DELETED("TASK_DELETED"),
    TASK_PROCESSED("TASK_PROCESSED"),
    TASK_REOPENED("TASK_REOPENED"),
    TASK_STAGE_CHANGED("TASK_STAGE_CHANGED"),
    TASK_MOVED_TO_DIRECTORY("TASK_MOVED_TO_DIRECTORY"),
    YOU_RESPONSIBLE("YOU_RESPONSIBLE"),
    YOU_OBSERVER("YOU_OBSERVER"),
    NEW_COMMENT("NEW_COMMENT"),
    TASK_HAS_BECOME_ACTUAL("TASK_HAS_BECOME_ACTUAL"),
    TASK_EXPIRED("TASK_EXPIRED"),
    WORKS_COMPLETED("WORKS_COMPLETED"),
    REPORT_RECEIVED("REPORT_RECEIVED"),
    MENTIONED_IN_TASK("MENTIONED_IN_TASK");

    private final String value;
    NotificationType(String value) {
        this.value = value;
    }

    public String getLabel() {
        return switch (this) {
            case NEW_COMMENT -> "Комментарий";
            case TASK_CREATED -> "Новая задача";
            case TASK_EDITED -> "Изменены детали задачи";
            case TASK_DELETED -> "Задача удалена";
            case TASK_CLOSED -> "Задача закрыта";
            case TASK_REOPENED -> "Задача возобновлена";
            case TASK_PROCESSED -> "Задача назначена";
            case TASK_STAGE_CHANGED -> "Тип задачи изменен";
            case TASK_MOVED_TO_DIRECTORY -> "Категория задачи изменена";
            case YOU_OBSERVER -> "Вы наблюдатель";
            case YOU_RESPONSIBLE -> "Вы ответственный";
            case TASK_HAS_BECOME_ACTUAL -> "Актуальная";
            case TASK_EXPIRED -> "Срок истек";
            case WORKS_COMPLETED -> "Работы завершены";
            case REPORT_RECEIVED -> "Отчет получен";
            case MENTIONED_IN_TASK -> "Вас упомянули";
            default -> "Неизвестный тип уведомления";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(NotificationType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromString(String value) {
        for (NotificationType type : NotificationType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new ResponseException("Неизвестный тип уведомления");
    }
}
