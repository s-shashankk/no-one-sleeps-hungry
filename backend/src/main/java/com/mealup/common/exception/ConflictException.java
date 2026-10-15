package com.mealup.common.exception;

import org.springframework.http.HttpStatus;

/** The request is valid but clashes with the current state (duplicate, invalid status change, ...). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
