package com.restaurant.inventory.dto;

import java.util.List;
import com.restaurant.inventory.helper.CsvParserHelper.MealCsvEntry;

public record ValidationResult(List<MealCsvEntry> validMeals, List<InvalidMealEntry> invalidMeals) {
}
