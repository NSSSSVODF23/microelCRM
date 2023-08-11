package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IllegalMediaType extends CustomException {
    public IllegalMediaType(String message) {
        super(message);
    }
}
