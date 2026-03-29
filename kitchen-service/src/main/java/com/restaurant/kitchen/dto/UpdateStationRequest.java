package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.Station;
import jakarta.validation.constraints.NotNull;

public record UpdateStationRequest(
        @NotNull(message = "station is required")
        Station station
) {}