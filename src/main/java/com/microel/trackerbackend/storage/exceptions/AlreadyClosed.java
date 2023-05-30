package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlreadyClosed extends CustomException{
    public AlreadyClosed(String message) {
        super(message);
    }
}
