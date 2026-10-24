package com.example.shortener.service;

import com.example.shortener.error.InvalidUrlException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Turns user input into a URL we are willing to store and redirect to.
 *
 * <p>Note that {@code new URI("not-a-url")} does NOT throw: a relative reference is a
 * perfectly valid URI. That is why the scheme and host are checked explicitly.</p>
 *
 * <p>Non-ASCII characters (a Chinese path or query, for instance) are legal in what a user
 * pastes but illegal in an RFC 3986 URI, so they are percent-encoded before parsing. Rejecting
 * them would be stricter than the requirements, which only ask for http(s), non-blank and
 * at most 2048 characters.</p>
 */
@Component
public class UrlValidator {

    public static final int MAX_LENGTH = 2048;

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    public String validateAndNormalize(String raw) {
        String value = raw == null ? "" : raw.trim();

        if (value.isEmpty()) {
            throw new InvalidUrlException("url must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidUrlException("url must be at most " + MAX_LENGTH + " characters");
        }

        String encoded = percentEncodeNonAscii(value);
        if (encoded.length() > MAX_LENGTH) {
            throw new InvalidUrlException("url must be at most " + MAX_LENGTH
                    + " characters once non-ASCII characters are percent-encoded");
        }

        URI uri;
        try {
            uri = new URI(encoded);
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

    /**
     * Encodes every code point above ASCII as UTF-8 percent escapes, leaving existing escapes
     * ({@code %2F}) and all ASCII characters untouched.
     */
    private static String percentEncodeNonAscii(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); ) {
            int codePoint = input.codePointAt(i);
            if (codePoint < 0x80) {
                out.append((char) codePoint);
            } else {
                byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    out.append('%').append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
                }
            }
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }
}
