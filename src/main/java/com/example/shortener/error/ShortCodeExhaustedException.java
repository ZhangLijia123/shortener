package com.example.shortener.error;

import org.springframework.http.HttpStatus;

/**
 * Raised when every generated code collided (or, more realistically, when the code space is
 * exhausted because {@code app.code-length} is too small for the traffic).
 */
public class ShortCodeExhaustedException extends ApiException {

    public ShortCodeExhaustedException(int attempts) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SHORT_CODE_UNAVAILABLE",
                "could not allocate a unique short code after " + attempts + " attempts");
    }
}
