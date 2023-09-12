package com.microel.trackerbackend.parsers.commutator.exceptions;

public class TelnetConnectionException extends RemoteAccessException {
    public TelnetConnectionException(String message) {
        super(message);
    }
}
