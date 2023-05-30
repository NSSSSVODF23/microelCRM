package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlreadyExists extends CustomException{
    public AlreadyExists(String message) {
        super(message);
    }
}
