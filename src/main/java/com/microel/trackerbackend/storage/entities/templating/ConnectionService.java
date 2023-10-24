package com.microel.trackerbackend.storage.entities.templating;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum ConnectionService {
    INTERNET("INTERNET"),
    CTV("CTV"),
    IPTV("IPTV");

    private final String value;

    ConnectionService(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return switch (this) {
            case INTERNET -> "Интернет";
            case CTV -> "КТВ";
            case IPTV -> "IPTV";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(ConnectionService.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }

    public static ConnectionService getByValue(String value) {
        return Stream.of(ConnectionService.values()).filter(service -> service.getValue().equals(value)).findFirst().orElseThrow();
    }

}
