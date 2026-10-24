package com.example.shortener.web;

import com.example.shortener.config.AppProperties;
import com.example.shortener.domain.Link;
import com.example.shortener.service.LinkService;
import com.example.shortener.service.StatsService;
import com.example.shortener.web.dto.CreateLinkRequest;
import com.example.shortener.web.dto.LinkDetailResponse;
import com.example.shortener.web.dto.LinkResponse;
import com.example.shortener.web.dto.LinkSummaryResponse;
import com.example.shortener.web.dto.StatsResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;
    private final StatsService statsService;
    private final AppProperties properties;

    public LinkController(LinkService linkService, StatsService statsService, AppProperties properties) {
        this.linkService = linkService;
        this.statsService = statsService;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        Link link = linkService.create(request.url());
        URI location = URI.create(properties.baseUrl() + "/" + link.getCode());
        return ResponseEntity.created(location).body(LinkResponse.from(link, properties.baseUrl()));
    }

    @GetMapping("/{code}")
    public LinkDetailResponse detail(@PathVariable String code) {
        return LinkDetailResponse.from(linkService.getByCode(code));
    }

    @GetMapping("/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return statsService.stats(code);
    }

    /** Convenience endpoint for the demo page: the 20 most recent links. */
    @GetMapping
    public List<LinkSummaryResponse> latest() {
        String baseUrl = properties.baseUrl();
        return linkService.latest().stream()
                .map(link -> LinkSummaryResponse.from(link, baseUrl))
                .toList();
    }
}
