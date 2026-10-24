package com.example.shortener.service;

import com.example.shortener.domain.Link;
import com.example.shortener.error.LinkNotFoundException;
import com.example.shortener.repo.LinkRepository;
import com.example.shortener.repo.VisitRepository;
import com.example.shortener.web.dto.DayCountDto;
import com.example.shortener.web.dto.StatsResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the "last 7 calendar days including today" series.
 */
@Service
public class StatsService {

    public static final int WINDOW_DAYS = 7;

    private final LinkRepository links;
    private final VisitRepository visits;
    private final Clock clock;
    private final ZoneId statsZone;

    public StatsService(LinkRepository links,
                        VisitRepository visits,
                        Clock clock,
                        ZoneId statsZone) {
        this.links = links;
        this.visits = visits;
        this.clock = clock;
        this.statsZone = statsZone;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(String code) {
        Link link = links.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));

        LocalDate today = LocalDate.now(clock.withZone(statsZone));
        LocalDate from = today.minusDays(WINDOW_DAYS - 1L);

        Map<LocalDate, Long> counts = new HashMap<>();
        for (VisitRepository.DayCount row : visits.countByDay(link.getId(), from, today)) {
            counts.put(row.getDay(), row.getCnt());
        }

        List<DayCountDto> days = new ArrayList<>(WINDOW_DAYS);
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            days.add(new DayCountDto(day, counts.getOrDefault(day, 0L)));
        }
        return new StatsResponse(code, days);
    }
}
