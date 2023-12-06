package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateCallbackHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateChatJoinHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class TelegramChatJoinReactor implements TelegramUpdateReactor{
    private TelegramUpdateChatJoinHandler handler;


    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.CHAT_JOIN_REQUEST;
    }

    @Override
    public TelegramUpdateChatJoinHandler getHandler() {
        return handler;
    }

}
