package com.microel.trackerbackend.controllers.configuration;

import com.microel.trackerbackend.CustomException;

public class FailedToReadConfigurationException extends CustomException {
    public FailedToReadConfigurationException(String message) {
        super(message);
    }
}
