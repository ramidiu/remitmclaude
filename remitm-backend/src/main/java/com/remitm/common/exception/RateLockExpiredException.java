package com.remitm.common.exception;

import org.springframework.http.HttpStatus;

public class RateLockExpiredException extends RemitmException {

    public RateLockExpiredException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public RateLockExpiredException(String quoteId, String expiryTime) {
        super(String.format("Rate lock for quote '%s' expired at '%s'", quoteId, expiryTime), HttpStatus.CONFLICT);
    }
}
