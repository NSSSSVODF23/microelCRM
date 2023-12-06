package com.microel.trackerbackend.controllers.telegram.reactor;

public enum TelegramReactorType {
    COMMAND("COMMAND"),
    PROMPT("PROMPT"),
    MESSAGE("MESSAGE"),
    EDIT_MESSAGE("EDIT_MESSAGE"),
    GROUP_MESSAGE("GROUP_MESSAGE"),
    GROUP_EDIT_MESSAGE("GROUP_EDIT_MESSAGE"),
    CHAT_JOIN_REQUEST("CHAT_JOIN_REQUEST"),
    CALLBACK("CALLBACK");
    private final String value;

    TelegramReactorType(String value) {
        this.value = value;
    }
}
