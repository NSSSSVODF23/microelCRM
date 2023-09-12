package com.microel.trackerbackend.parsers.commutator.exceptions;

public class AuthorizationException extends RemoteAccessException {
    public AuthorizationException(String message) {
        super(message);
    }
}
