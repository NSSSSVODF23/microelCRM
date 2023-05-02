package com.microel.trackerbackend.controllers.configuration;

public class FailedToWriteConfigurationException extends Exception{
    public FailedToWriteConfigurationException(String message) {
        super(message);
    }
}
