package com.restaurant.menu.dto;

import java.util.List;
import java.util.Map;

public record MealDTO(
        Long id,
        String name,
        List<Map<String, Object>> nutrients,
        Double price,
        String category,
        Boolean isActive,
        List<IngredientDTO> ingredients) {
}
