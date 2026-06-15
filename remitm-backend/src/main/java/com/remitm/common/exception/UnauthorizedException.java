package com.remitm.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends RemitmException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
