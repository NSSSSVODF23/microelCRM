package com.microel.trackerbackend.services.external.billing;

import com.microel.trackerbackend.CustomException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BillingAuthenticationException extends CustomException {
    public BillingAuthenticationException(String message) {
        super(message);
    }
}
