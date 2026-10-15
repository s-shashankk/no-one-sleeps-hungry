package com.mealup.common.exception;

import org.springframework.http.HttpStatus;

/** Base class for errors we raise on purpose; the handler turns them into the standard JSON error. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
