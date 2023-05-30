package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlreadyDeleted extends CustomException{
    public AlreadyDeleted(String message) {
        super(message);
    }
}
