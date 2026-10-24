package com.example.shortener.web.dto;

import com.example.shortener.domain.Link;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Body of {@code 200 OK} for {@code GET /api/links/{code}}. */
public record LinkDetailResponse(
        String code,
        String url,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("visit_count") long visitCount) {

    public static LinkDetailResponse from(Link link) {
        return new LinkDetailResponse(
                link.getCode(),
                link.getUrl(),
                link.getCreatedAt(),
                link.getVisitCount());
    }
}
