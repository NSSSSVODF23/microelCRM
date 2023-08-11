package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EmptyResponse extends CustomException {
    public EmptyResponse(String message) {
        super(message);
    }
}
