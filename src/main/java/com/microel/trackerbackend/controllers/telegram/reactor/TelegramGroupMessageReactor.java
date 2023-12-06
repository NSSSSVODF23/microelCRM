package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramGroupMessageReactor implements TelegramUpdateReactor {

    private TelegramUpdateMessageHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.GROUP_MESSAGE;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }
}
