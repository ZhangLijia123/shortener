package com.example.shortener.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors we deliberately raise. Carrying the HTTP status and the stable
 * machine-readable code together is what keeps every endpoint's error body identical.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
