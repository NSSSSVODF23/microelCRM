package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class CustomException extends Exception{
    public CustomException(String message) {
        super(message);
    }
}
