package com.restaurant.kitchen.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDisplayNameRequest(
        @NotBlank(message = "displayName is required")
        String displayName
) {}