package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.ChefStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "status is required")
        ChefStatus status
) {}