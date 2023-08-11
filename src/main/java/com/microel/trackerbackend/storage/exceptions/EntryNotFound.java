package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EntryNotFound extends CustomException {
    public EntryNotFound(String message) {
        super(message);
    }
}
