package com.microel.trackerbackend.controllers.telegram;

public enum TelegramReactorType {
    COMMAND("COMMAND"),
    PROMPT("PROMPT");
    private final String value;
    TelegramReactorType(String value) {
        this.value = value;
    }
}
