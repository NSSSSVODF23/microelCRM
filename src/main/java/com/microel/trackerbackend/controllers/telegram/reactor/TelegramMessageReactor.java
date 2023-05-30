package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramMessageReactor implements TelegramUpdateReactor {

    private TelegramUpdateMessageHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.MESSAGE;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }
}
