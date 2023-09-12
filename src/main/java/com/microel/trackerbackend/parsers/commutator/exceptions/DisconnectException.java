package com.microel.trackerbackend.parsers.commutator.exceptions;

public class DisconnectException extends RemoteAccessException {
    public DisconnectException(String message) {
        super(message);
    }
}
