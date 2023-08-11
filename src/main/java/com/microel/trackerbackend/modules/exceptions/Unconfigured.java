package com.microel.trackerbackend.modules.exceptions;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Unconfigured extends CustomException {
    public Unconfigured(String message) {super(message);}
}
