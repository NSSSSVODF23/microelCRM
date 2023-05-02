package com.microel.trackerbackend.controllers.telegram;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramPromptReactor implements TelegramUpdateReactor {

    private TelegramUpdateHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.PROMPT;
    }

    @Override
    public TelegramUpdateHandler getHandler() {
        return handler;
    }
}
