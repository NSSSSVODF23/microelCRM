package com.microel.trackerbackend.misc.autosupport.schema.predicates;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum PredicateType {
    USER_CREDENTIALS("USER_CREDENTIALS"),
    AUTH_USER("AUTH_USER"),
    POSITIVE_BALANCE("POSITIVE_BALANCE"),
    DEFERRED_PAYMENT("DEFERRED_PAYMENT"),
    HAS_AUTH_HARDWARE("HAS_AUTH_HARDWARE"),
    HAS_ONLINE_HARDWARE("HAS_ONLINE_HARDWARE"),
    IS_LARGE_UPTIME("IS_LARGE_UPTIME");

    private final String value;

    PredicateType(String value) {
        this.value = value;
    }

    public String getLabel() {
        return switch (this) {
            case USER_CREDENTIALS -> "Правильность ЛП";
            case AUTH_USER -> "Авторизовать пользователя";
            case POSITIVE_BALANCE -> "Положительный баланс";
            case DEFERRED_PAYMENT -> "Включить отложенный платеж";
            case HAS_AUTH_HARDWARE -> "Наличие авторизованного устройства";
            case HAS_ONLINE_HARDWARE -> "Устройство в сети";
            case IS_LARGE_UPTIME -> "Большой uptime устройства";
            default -> "Неизвестный тип уведомления";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(PredicateType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }
}
