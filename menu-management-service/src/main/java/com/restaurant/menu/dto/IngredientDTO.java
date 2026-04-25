package com.restaurant.menu.dto;

import java.util.List;
import java.util.Map;

public record IngredientDTO(
        Long id,
        String name,
        String description,
        List<Map<String, Object>> nutrients,
        Integer stock) {
}
