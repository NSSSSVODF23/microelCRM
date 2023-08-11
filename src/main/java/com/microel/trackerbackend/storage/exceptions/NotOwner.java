package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;

public class NotOwner extends CustomException {
    public NotOwner(String message) {
        super(message);
    }
}
