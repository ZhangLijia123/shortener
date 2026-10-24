package com.example.shortener.web.dto;

import java.time.LocalDate;

public record DayCountDto(LocalDate date, long count) {
}
