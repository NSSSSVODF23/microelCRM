package com.microel.trackerbackend.controllers.telegram;

import java.util.UUID;

public interface TelegramUpdateReactor {
    TelegramReactorType getType();
    TelegramUpdateHandler getHandler();
}
