package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;

public class IllegalFields extends CustomException {
    public IllegalFields(String message) {
        super(message);
    }
}
