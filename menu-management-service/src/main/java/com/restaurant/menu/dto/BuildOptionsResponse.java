package com.restaurant.menu.dto;

import java.util.List;

public record BuildOptionsResponse(
        String primaryCategory,
        List<SlotOptions> slots
) {
    public record SlotOptions(
            String slotName,
            String ingredientCategory,
            Boolean isRequired,
            Integer maxSelect,
            List<IngredientDTO> ingredients
    ) {}
}
