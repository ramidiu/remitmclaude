package com.remitm.common.exception;

import org.springframework.http.HttpStatus;

public class ComplianceHoldException extends RemitmException {

    public ComplianceHoldException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
