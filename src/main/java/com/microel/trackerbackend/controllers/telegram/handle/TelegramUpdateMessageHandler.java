package com.microel.trackerbackend.controllers.telegram.handle;

import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FunctionalInterface
public interface TelegramUpdateMessageHandler extends TelegramUpdateHandler {
    boolean handle(Update update) throws EntryNotFound, TelegramApiException, IllegalFields;
}
