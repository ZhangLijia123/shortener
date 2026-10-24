package com.example.shortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 2048, message = "must be at most 2048 characters")
        String url) {
}
