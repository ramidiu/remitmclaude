package com.remitm.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFloatException extends RemitmException {

    public InsufficientFloatException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
