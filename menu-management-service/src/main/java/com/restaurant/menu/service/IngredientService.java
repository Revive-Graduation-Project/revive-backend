package com.restaurant.menu.service;

import com.restaurant.menu.dto.BulkUpdateStockRequest.StockEntry;
import com.restaurant.menu.dto.IngredientNutrition;
import com.restaurant.menu.dto.ReserveMealDTO;
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
     * Creates a new ingredient by resolving its nutrition via inventory-service.
     */
    IngredientDTO createIngredient(String name);

    /**
     * Deletes an ingredient by its ID.
     */
    void deleteIngredient(Long id);

    /**
     * Updates the stock quantity for a single ingredient.
     */
    IngredientDTO updateStock(Long id, Double stock);

    /**
     * Bulk updates stock quantities for multiple ingredients.
     */
    List<IngredientDTO> bulkUpdateStock(List<StockEntry> updates);

    /**
     * Reserves ingredients for a meal order.
     * Applies pessimistic locking on affected ingredient rows to prevent
     * concurrent orders from double-deducting stock.
     *
     * @param meals list of meals with quantities to reserve
     * @return updated ingredient stock after reservation
     */
    List<IngredientDTO> reserveIngredients(List<ReserveMealDTO> meals);

    /**
     * Reverts a previously reserved stock deduction (e.g., when an order fails).
     * Adds the ingredient quantities back to stock.
     *
     * @param meals the same list of meals that was originally reserved
     */
    void revertIngredients(List<ReserveMealDTO> meals);
}
