package com.example.shortener.error;

import org.springframework.http.HttpStatus;

public class LinkNotFoundException extends ApiException {

    public LinkNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "no link found for code '" + code + "'");
    }
}
