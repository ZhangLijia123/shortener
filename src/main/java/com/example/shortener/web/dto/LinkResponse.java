package com.example.shortener.web.dto;

import com.example.shortener.domain.Link;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Body of {@code 201 Created} for {@code POST /api/links}. */
public record LinkResponse(
        String code,
        @JsonProperty("short_url") String shortUrl,
        String url,
        @JsonProperty("created_at") Instant createdAt) {

    public static LinkResponse from(Link link, String baseUrl) {
        return new LinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getUrl(),
                link.getCreatedAt());
    }
}
