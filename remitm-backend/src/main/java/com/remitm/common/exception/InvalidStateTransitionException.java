package com.remitm.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends RemitmException {

    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public InvalidStateTransitionException(String fromState, String toState) {
        super(String.format("Invalid state transition from '%s' to '%s'", fromState, toState), HttpStatus.BAD_REQUEST);
    }
}
