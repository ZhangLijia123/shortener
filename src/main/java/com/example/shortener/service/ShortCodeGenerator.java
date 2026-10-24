package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates 6..8 character codes from the 62-character alphabet {@code [a-zA-Z0-9]}.
 *
 * <p>7 characters give 62^7 ≈ 3.52e12 possible codes, which at this service's scale makes a
 * collision a curiosity rather than a routine event (about 3e-9 per insert with 10k links).
 * {@link SecureRandom} is used so codes cannot be predicted from previously issued ones.</p>
 *
 * <p>This class only *proposes* codes. Uniqueness is enforced by {@code uk_links_code}.</p>
 */
@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(AppProperties properties) {
        this.length = properties.codeLength();
    }

    public String next() {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buffer);
    }
}
