package com.microel.trackerbackend.storage.entities.templating;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum AdvertisingSource {
    RESUMPTION("RESUMPTION"),
    LOSS("LOSS"),
    MAIL("MAIL"),
    LEAFLET("LEAFLET"),
    SOUND("SOUND"),
    RADIO("RADIO"),
    SOCIALNET("SOCIALNET"),
    BANNER("BANNER"),
    KITH("KITH"),
    SMS("SMS"),
    INTERNET("INTERNET"),
    MANAGER("MANAGER"),
    EARLYUSED("EARLYUSED"),
    AGENT("AGENT"),
    ONPILLAR("ONPILLAR");

    private final String value;

    AdvertisingSource(String value) {
        this.value = value;
    }

    public String getLabel() {
        return switch (this) {
            case RESUMPTION -> "Возобновление/Перенос";
            case LOSS -> "Затрудняюсь";
            case MAIL -> "Письмо";
            case LEAFLET -> "Листовка";
            case SOUND -> "Звуковая реклама";
            case RADIO -> "Радио";
            case SOCIALNET -> "Социальные сети";
            case BANNER -> "Баннер на улице";
            case KITH -> "Знакомые";
            case SMS -> "СМС";
            case INTERNET -> "Интернет";
            case MANAGER -> "Менеджер";
            case EARLYUSED -> "Ранее пользовался";
            case AGENT -> "Агент";
            case ONPILLAR -> "Реклама на столбе";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(AdvertisingSource.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
    }
}
