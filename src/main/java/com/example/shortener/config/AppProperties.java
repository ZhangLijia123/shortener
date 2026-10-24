package com.example.shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalised, validated configuration.
 *
 * <p>Everything that a reviewer might want to change (public base URL, the time zone that
 * defines a "day", how long codes are) lives here so it can be overridden with environment
 * variables without touching code.</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("UTC") String statsZone,
        @DefaultValue("7") int codeLength,
        @DefaultValue("5") int codeMaxAttempts) {

    public AppProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8080";
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (statsZone == null || statsZone.isBlank()) {
            statsZone = "UTC";
        }
        if (codeLength < 6 || codeLength > 8) {
            throw new IllegalArgumentException("app.code-length must be between 6 and 8, got " + codeLength);
        }
        if (codeMaxAttempts < 1) {
            throw new IllegalArgumentException("app.code-max-attempts must be >= 1, got " + codeMaxAttempts);
        }
    }
}
