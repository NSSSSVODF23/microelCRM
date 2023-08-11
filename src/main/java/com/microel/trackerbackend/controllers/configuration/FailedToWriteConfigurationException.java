package com.microel.trackerbackend.controllers.configuration;

import com.microel.trackerbackend.CustomException;

public class FailedToWriteConfigurationException extends CustomException {
    public FailedToWriteConfigurationException(String message) {
        super(message);
    }
}
