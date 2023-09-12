package com.microel.trackerbackend.parsers.commutator.exceptions;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public abstract class RemoteAccessException extends RuntimeException {
    public RemoteAccessException(String message) {
        super(message);
    }
}
