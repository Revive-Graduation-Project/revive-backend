package com.restaurant.inventory.dto;

import java.util.List;

public record UsdaFoodDetail(int fdcId, String originalName, String description, List<NutrientInfo> foodNutrients) {}