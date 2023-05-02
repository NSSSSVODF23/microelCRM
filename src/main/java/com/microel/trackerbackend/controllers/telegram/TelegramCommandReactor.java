package com.microel.trackerbackend.controllers.telegram;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class TelegramCommandReactor implements TelegramUpdateReactor{

    private String targetCommand;
    private TelegramUpdateHandler handler;


    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.COMMAND;
    }

    @Override
    public TelegramUpdateHandler getHandler() {
        return handler;
    }

}
