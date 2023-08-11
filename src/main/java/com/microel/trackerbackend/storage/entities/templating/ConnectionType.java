package com.microel.trackerbackend.storage.entities.templating;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum ConnectionType {
    NEW("NEW"),
    RESUMPTION("RESUMPTION"),
    TRANSFER("TRANSFER");

    private final String value;

    ConnectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return switch (this) {
            case NEW -> "Новое";
            case RESUMPTION -> "Возобновление";
            case TRANSFER -> "Перенос";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(ConnectionType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }
}
