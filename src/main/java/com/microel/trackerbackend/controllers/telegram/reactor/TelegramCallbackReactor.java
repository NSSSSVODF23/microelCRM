package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateCallbackHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class TelegramCallbackReactor implements TelegramUpdateReactor{

    @Nullable
    private String prefix = null;
    private TelegramUpdateCallbackHandler handler;


    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.CALLBACK;
    }

    @Override
    public TelegramUpdateCallbackHandler getHandler() {
        return handler;
    }

}
