package com.restaurant.menu.service;

import com.restaurant.menu.dto.BulkUpdateStockRequest.StockEntry;
import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.entity.Ingredient;

import java.util.List;

public interface IngredientService {

    /**
     * Resolves an existing ingredient by name or creates a new one from
     * the nutrition event payload. Used internally during RabbitMQ processing.
     */
    Ingredient resolveOrSaveIngredient(IngredientNutrition ingDto);

    /**
     * Retrieves all ingredients.
     */
    List<IngredientDTO> getAllIngredients();

    /**
     * Retrieves a single ingredient by its ID.
     */
    IngredientDTO getIngredientById(Long id);

    /**
     * Updates the stock quantity for a single ingredient.
     */
    IngredientDTO updateStock(Long id, Integer stock);

    /**
     * Bulk updates stock quantities for multiple ingredients.
     */
    List<IngredientDTO> bulkUpdateStock(List<StockEntry> updates);
}
