package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateCallbackHandler;
import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
public class TelegramPreCheckoutReactor implements TelegramUpdateReactor{

    private TelegramUpdateMessageHandler handler;


    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.PRE_CHECKOUT;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }

}
