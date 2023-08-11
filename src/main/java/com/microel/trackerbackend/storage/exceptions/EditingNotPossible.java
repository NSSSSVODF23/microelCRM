package com.microel.trackerbackend.storage.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EditingNotPossible extends CustomException {
    public EditingNotPossible(String message) {
        super(message);
    }
}
