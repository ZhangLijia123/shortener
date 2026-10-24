package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.domain.Link;
import com.example.shortener.error.LinkNotFoundException;
import com.example.shortener.error.ShortCodeExhaustedException;
import com.example.shortener.repo.LinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

    private static final Logger log = LoggerFactory.getLogger(LinkService.class);

    private final ShortCodeGenerator generator;
    private final LinkInserter inserter;
    private final UrlValidator validator;
    private final LinkRepository links;
    private final AppProperties properties;
    private final Clock clock;

    public LinkService(ShortCodeGenerator generator,
                       LinkInserter inserter,
                       UrlValidator validator,
                       LinkRepository links,
                       AppProperties properties,
                       Clock clock) {
        this.generator = generator;
        this.inserter = inserter;
        this.validator = validator;
        this.links = links;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Creates a link. Duplicate URLs are allowed and get a new code every time (see README).
     */
    public Link create(String rawUrl) {
        String url = validator.validateAndNormalize(rawUrl);
        Instant createdAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        int attempts = properties.codeMaxAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String code = generator.next();
            try {
                return inserter.insertOnce(code, url, createdAt);
            } catch (DataIntegrityViolationException collision) {
                log.warn("short code collision on attempt {}/{} (code={}), retrying", attempt, attempts, code);
            }
        }
        throw new ShortCodeExhaustedException(attempts);
    }

    @Transactional(readOnly = true)
    public Link getByCode(String code) {
        return links.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
    }

    @Transactional(readOnly = true)
    public List<Link> latest() {
        return links.findTop20ByOrderByCreatedAtDescIdDesc();
    }
}
