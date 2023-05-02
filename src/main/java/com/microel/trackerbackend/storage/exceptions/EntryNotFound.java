package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EntryNotFound extends Exception {
    public EntryNotFound(String message) {
        super(message);
    }
}
