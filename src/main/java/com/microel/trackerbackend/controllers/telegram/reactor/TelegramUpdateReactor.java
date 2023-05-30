package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateHandler;

public interface TelegramUpdateReactor {
    TelegramReactorType getType();
    TelegramUpdateHandler getHandler();
}
