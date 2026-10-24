package com.example.shortener.service;

import com.example.shortener.error.InvalidUrlException;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Component;

/**
 * Turns user input into a URL we are willing to store and redirect to.
 *
 * <p>Note that {@code new URI("not-a-url")} does NOT throw: a relative reference is a
 * perfectly valid URI. That is why the scheme and host are checked explicitly.</p>
 */
@Component
public class UrlValidator {

    public static final int MAX_LENGTH = 2048;

    public String validateAndNormalize(String raw) {
        String value = raw == null ? "" : raw.trim();

        if (value.isEmpty()) {
            throw new InvalidUrlException("url must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidUrlException("url must be at most " + MAX_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("url is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("url must be a valid http/https URL");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("url must have a host");
        }
        return uri.toString();
    }
}
