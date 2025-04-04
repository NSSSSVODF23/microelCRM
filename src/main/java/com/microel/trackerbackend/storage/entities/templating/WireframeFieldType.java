package com.microel.trackerbackend.storage.entities.templating;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public enum WireframeFieldType {
    BOOLEAN("BOOLEAN"),
    SMALL_TEXT("SMALL_TEXT"),
    LARGE_TEXT("LARGE_TEXT"),
    INTEGER("INTEGER"),
    FLOAT("FLOAT"),
    ADDRESS("ADDRESS"),
    LOGIN("LOGIN"),
    AD_SOURCE("AD_SOURCE"),
    REQUEST_INITIATOR("REQUEST_INITIATOR"),
    IP("IP"),
    EQUIPMENTS("EQUIPMENTS"),
    CONNECTION_SERVICES("CONNECTION_SERVICES"),
    CONNECTION_TYPE("CONNECTION_TYPE"),
    PHONE_ARRAY("PHONE_ARRAY"),
    PASSPORT_DETAILS("PASSPORT_DETAILS"),
    COUNTING_LIVES("COUNTING_LIVES");

    private final String name;

    WireframeFieldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // todo Для добавления типа поля, нужно добавить сюда1
    public String getLabel() {
        return switch (name) {
            case "BOOLEAN" -> "Логическое";
            case "SMALL_TEXT" -> "Строка текста";
            case "LARGE_TEXT" -> "Текст";
            case "INTEGER" -> "Целое число";
            case "FLOAT" -> "Дробное число";
            case "ADDRESS" -> "Адрес";
            case "LOGIN" -> "Логин";
            case "AD_SOURCE" -> "Рекламный источник";
            case "REQUEST_INITIATOR" -> "Инициатор задачи";
            case "IP" -> "IP Адрес";
            case "EQUIPMENTS" -> "Абонентское оборудование";
            case "CONNECTION_SERVICES" -> "Подключаемые услуги";
            case "CONNECTION_TYPE" -> "Тип подключения";
            case "PHONE_ARRAY" -> "Телефонные номера";
            case "COUNTING_LIVES" -> "Количество живых";
            case "PASSPORT_DETAILS" -> "Паспортные данные";
            default -> "НЕИЗВЕСТНЫЙ ТИП";
        };
    }

    public static List<Map<String,String>> getList(){
        return Stream.of(WireframeFieldType.values()).map(value->Map.of("label", value.getLabel(), "value", value.getName())).toList();
    }

    @Override
    public String toString() {
        return name;
    }
}
