package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SaveEntryFailed extends CustomException {
    public SaveEntryFailed(String message) {
        super(message);
    }
}
