package com.restaurant.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
        @NotBlank(message = "displayName is required")
        @Size(min = 4 , max = 12 , message = "displayName must be between 4 and 12 characters")
        String displayName
) {}