package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlreadyExists extends CustomException {
    public AlreadyExists(String message) {
        super(message);
    }
}
