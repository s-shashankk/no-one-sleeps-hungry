package com.mealup.common.exception;

import org.springframework.http.HttpStatus;

/** The caller is not logged in, or the login details are wrong. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
