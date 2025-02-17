package com.microel.trackerbackend.controllers.telegram.reactor;

import com.microel.trackerbackend.controllers.telegram.handle.TelegramUpdateMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TelegramSuccessfulPaymentReactor implements TelegramUpdateReactor {

    private TelegramUpdateMessageHandler handler;

    @Override
    public TelegramReactorType getType() {
        return TelegramReactorType.SUCCESSFUL_PAYMENT;
    }

    @Override
    public TelegramUpdateMessageHandler getHandler() {
        return handler;
    }
}
