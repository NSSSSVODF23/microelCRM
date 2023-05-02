package com.microel.trackerbackend.controllers.telegram;

import org.telegram.telegrambots.meta.api.objects.Update;

@FunctionalInterface
public interface TelegramUpdateHandler {
    void handle(Update update);
}
