package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TelegramBotNotInitialized extends CustomException {
    public TelegramBotNotInitialized(String message) {
        super(message);
    }
}
