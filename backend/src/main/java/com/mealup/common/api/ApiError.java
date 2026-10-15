package com.mealup.common.api;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

/** The one error format used by every endpoint. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldIssue> fieldErrors) {

    public record FieldIssue(String field, String message) { }

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldIssue> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
