package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramGroupEditMessageReactor implements TelegramUpdateReactor {

    private TelegramUpdateMessageHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.GROUP_EDIT_MESSAGE;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }
}
