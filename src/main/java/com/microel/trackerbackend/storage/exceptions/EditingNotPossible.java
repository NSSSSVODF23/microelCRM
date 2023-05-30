package com.microel.trackerbackend.storage.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EditingNotPossible extends CustomException{
    public EditingNotPossible(String message) {
        super(message);
    }
}
