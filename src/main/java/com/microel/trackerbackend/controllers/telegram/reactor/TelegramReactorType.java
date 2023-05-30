package com.microel.trackerbackend.controllers.telegram.reactor;

public enum TelegramReactorType {
    COMMAND("COMMAND"),
    PROMPT("PROMPT"),
    MESSAGE("MESSAGE"),
    EDIT_MESSAGE("EDIT_MESSAGE"),
    CALLBACK("CALLBACK");
    private final String value;

    TelegramReactorType(String value) {
        this.value = value;
    }
}
