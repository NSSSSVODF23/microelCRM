package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramPromptReactor implements TelegramUpdateReactor {

    private String targetPrompt;

    private TelegramUpdateMessageHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.PROMPT;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }
}
