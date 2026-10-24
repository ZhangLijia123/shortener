package com.example.shortener.web.dto;

import com.example.shortener.domain.Link;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Item shape of {@code GET /api/links} (used by the demo UI). */
public record LinkSummaryResponse(
        String code,
        @JsonProperty("short_url") String shortUrl,
        String url,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("visit_count") long visitCount) {

    public static LinkSummaryResponse from(Link link, String baseUrl) {
        return new LinkSummaryResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getUrl(),
                link.getCreatedAt(),
                link.getVisitCount());
    }
}
