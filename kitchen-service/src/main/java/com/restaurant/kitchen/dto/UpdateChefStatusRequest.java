package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.ChefStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateChefStatusRequest(
        @NotNull(message = "status is required")
        ChefStatus status
) {}