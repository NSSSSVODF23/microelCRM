package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ExceptionInsideThread extends CustomException{
    public ExceptionInsideThread(String message) {
        super(message);
    }
}
