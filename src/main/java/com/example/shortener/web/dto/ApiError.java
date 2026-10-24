package com.example.shortener.web.dto;

/**
 * The one and only error body shape used by every endpoint.
 */
public record ApiError(int status, String error, String message) {
}
