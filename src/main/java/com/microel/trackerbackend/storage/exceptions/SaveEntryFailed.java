package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SaveEntryFailed extends CustomException{
    public SaveEntryFailed(String message) {
        super(message);
    }
}
