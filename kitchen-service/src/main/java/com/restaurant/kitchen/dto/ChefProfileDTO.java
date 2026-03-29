package com.restaurant.kitchen.dto;

import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.Station;

public record ChefProfileDTO(
        Long id,
        Long authUserId,
        String displayName,
        Station station,
        ChefStatus status
) {}
