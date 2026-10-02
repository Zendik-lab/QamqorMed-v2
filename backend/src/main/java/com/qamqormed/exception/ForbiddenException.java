package com.qamqormed.exception;

import org.springframework.http.HttpStatus;

/** Used when an authenticated patient is not allowed to access another subject's medical data. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
