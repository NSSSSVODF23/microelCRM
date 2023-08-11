package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EmptyResponse extends CustomException{
    public EmptyResponse(String message) {
        super(message);
    }
}
