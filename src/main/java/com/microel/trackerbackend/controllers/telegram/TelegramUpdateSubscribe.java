package com.microel.trackerbackend.controllers.telegram;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TelegramUpdateSubscribe {
    private MainBot botInstance;
    private UUID subscriptionId;
    private TelegramUpdateReactor reactor;
    private Boolean isOnce;

    public void unsubscribe() {
        botInstance.unsubscribe(subscriptionId);
    }
}
