package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlreadyDeleted extends CustomException {
    public AlreadyDeleted(String message) {
        super(message);
    }
}
