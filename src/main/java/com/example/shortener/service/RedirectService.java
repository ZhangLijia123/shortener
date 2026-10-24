package com.example.shortener.service;

import com.example.shortener.domain.Link;
import com.example.shortener.domain.Visit;
import com.example.shortener.error.LinkNotFoundException;
import com.example.shortener.repo.LinkRepository;
import com.example.shortener.repo.VisitRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The hot path: resolve a code and record the visit.
 */
@Service
public class RedirectService {

    private final LinkRepository links;
    private final VisitRepository visits;
    private final Clock clock;
    private final ZoneId statsZone;

    public RedirectService(LinkRepository links,
                           VisitRepository visits,
                           Clock clock,
                           ZoneId statsZone) {
        this.links = links;
        this.visits = visits;
        this.clock = clock;
        this.statsZone = statsZone;
    }

    /**
     * Resolves the target URL and records the visit.
     *
     * <p>Both writes happen in one transaction (2 statements, no remote calls), so the total
     * counter and the daily series can never disagree: either both land or neither does.</p>
     *
     * <p>The counter is a single atomic UPDATE, not a read-modify-write, which is what makes
     * concurrent redirects safe. Note the absence of {@code readOnly = true}: it would silently
     * make the UPDATE a no-op.</p>
     */
    @Transactional
    public String resolveAndCount(String code) {
        Link link = links.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));

        int updated = links.incrementVisitCount(link.getId());
        if (updated == 0) {
            // The link vanished between the SELECT and the UPDATE.
            throw new LinkNotFoundException(code);
        }

        Instant now = clock.instant();
        visits.save(new Visit(link, LocalDate.now(clock.withZone(statsZone)), now));

        return link.getUrl();
    }
}
