package com.example.shortener.service;

import com.example.shortener.domain.Link;
import com.example.shortener.repo.LinkRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs exactly one insert attempt in its own transaction.
 *
 * <p>This separate bean exists for one reason: a unique-constraint violation marks the current
 * transaction as rollback-only in Hibernate, so a retry loop that swallowed the exception
 * inside the same transaction would then fail on every subsequent operation with
 * "Transaction marked as rollback-only". With {@link Propagation#REQUIRES_NEW} each attempt
 * gets a fresh transaction and a failed attempt costs nothing but a log line.</p>
 */
@Service
public class LinkInserter {

    private final LinkRepository links;

    public LinkInserter(LinkRepository links) {
        this.links = links;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link insertOnce(String code, String url, Instant createdAt) {
        // saveAndFlush forces the INSERT now, so a collision surfaces here and not at commit time.
        return links.saveAndFlush(new Link(code, url, createdAt));
    }
}
