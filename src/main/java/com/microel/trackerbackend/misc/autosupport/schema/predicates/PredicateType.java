package com.microel.trackerbackend.misc.autosupport.schema.predicates;

import com.microel.trackerbackend.storage.entities.team.notification.NotificationType;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum PredicateType {
    USER_CREDENTIALS("USER_CREDENTIALS"),
    AUTH_USER("AUTH_USER");

    private final String value;

    PredicateType(String value) {
        this.value = value;
    }

    public String getLabel() {
        return switch (this) {
            case USER_CREDENTIALS -> "Проверить ЛП пользователя";
            case AUTH_USER -> "Авторизовать пользователя";
            default -> "Неизвестный тип уведомления";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(PredicateType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }
}
