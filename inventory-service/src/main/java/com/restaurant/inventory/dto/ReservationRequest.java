package com.restaurant.inventory.dto;

import java.util.Map;
import java.util.UUID;

public record ReservationRequest(
        Map<UUID, Double> items // ingredientId -> grams
) {}
