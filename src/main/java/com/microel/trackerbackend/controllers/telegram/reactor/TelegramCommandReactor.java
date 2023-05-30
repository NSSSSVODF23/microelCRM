package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class TelegramCommandReactor implements TelegramUpdateReactor{

    private String targetCommand;
    private TelegramUpdateMessageHandler handler;


    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.COMMAND;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }

}
