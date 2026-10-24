package com.example.shortener.web.dto;

import java.util.List;

/** Body of {@code 200 OK} for {@code GET /api/links/{code}/stats}. */
public record StatsResponse(String code, List<DayCountDto> days) {
}
