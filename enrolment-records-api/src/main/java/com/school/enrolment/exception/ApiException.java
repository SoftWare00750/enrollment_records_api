package com.school.enrolment.exception;

import org.springframework.http.HttpStatus;

/** A business-rule failure that maps cleanly to an HTTP status. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    private ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** The request is well-formed but conflicts with current state (duplicate, class full, ...). */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /** The request is well-formed but violates a business rule (wrong status, closed intake, ...). */
    public static ApiException unprocessable(String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
