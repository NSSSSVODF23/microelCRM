package com.microel.trackerbackend.services.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ResponseException extends RuntimeException {
    public ResponseException(String error) {
        super(error);
    }
}
