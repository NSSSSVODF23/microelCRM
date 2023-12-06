package com.microel.trackerbackend.controllers.telegram.handle;

import com.microel.trackerbackend.controllers.telegram.CallbackData;
import com.microel.trackerbackend.storage.exceptions.AlreadyClosed;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@FunctionalInterface
public interface TelegramUpdateChatJoinHandler extends TelegramUpdateHandler {
    boolean handle(Update update) throws EntryNotFound, IllegalFields, AlreadyClosed, TelegramApiException;
}
