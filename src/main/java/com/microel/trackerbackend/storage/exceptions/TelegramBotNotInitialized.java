package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TelegramBotNotInitialized extends CustomException{
    public TelegramBotNotInitialized(String message) {
        super(message);
    }
}
