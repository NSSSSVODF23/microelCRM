package com.microel.trackerbackend.misc.autosupport.schema.preprocessors;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum PreprocessorType {
    USER_INFO("USER_INFO");

    private final String value;

    PreprocessorType(String value) {
        this.value = value;
    }

    public String getLabel() {
        return switch (this) {
            case USER_INFO -> "Получить информацию о пользователе";
            default -> "Неизвестный тип уведомления";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(PreprocessorType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }
}
