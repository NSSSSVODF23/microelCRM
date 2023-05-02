package com.microel.trackerbackend.controllers.configuration;

public class FailedToReadConfigurationException extends Exception {
    public FailedToReadConfigurationException(String message) {
        super(message);
    }
}
