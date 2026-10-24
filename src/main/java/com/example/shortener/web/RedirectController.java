package com.example.shortener.web;

import com.example.shortener.error.LinkNotFoundException;
import com.example.shortener.service.RedirectService;
import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    /**
     * Same rule as {@code app.code-length}: 6..8 characters from {@code [a-zA-Z0-9]}.
     *
     * <p>This is a guard, not the source of truth — an invalid-shaped path is treated exactly
     * like an unknown code (404). Doing the check here rather than with a regex quantifier in
     * the mapping keeps the mapping portable and makes the rule visible in Java.</p>
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9]{6,8}");

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    /**
     * 302 Found, not 301.
     *
     * <p>A 301 is cached by browsers and proxies, so repeat visits never reach the server and
     * the visit statistics would silently undercount. {@code no-store} makes the intent explicit
     * even for intermediaries.</p>
     *
     * <p>The {@code \w+} regex keeps {@code /} (the welcome page) out of this handler — a bare
     * {@code {code}} template matches the root path with an empty value and shadows the static
     * index.html. PathPattern rejects a literal {@code /} inside a variable regex, so
     * {@code \w+} (which cannot match a separator) is used instead.</p>
     */
    @GetMapping("/{code:\\w+}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new LinkNotFoundException(code);
        }

        String target = redirectService.resolveAndCount(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
