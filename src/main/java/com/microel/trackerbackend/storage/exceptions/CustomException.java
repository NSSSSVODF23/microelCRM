package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public abstract class CustomException extends RuntimeException{
    public CustomException(String message) {
        super(message);
    }
}
