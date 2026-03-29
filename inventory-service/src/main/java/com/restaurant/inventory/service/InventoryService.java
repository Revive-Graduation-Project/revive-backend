package com.restaurant.inventory.service;

import com.restaurant.inventory.entity.Ingredient;
import java.util.Map;
import java.util.UUID;

public interface InventoryService {
    Ingredient getIngredientById(UUID id);
    void reserveStock(Map<UUID, Double> items);
    void commitStock(Map<UUID, Double> items);
    void rollbackStock(Map<UUID, Double> items);
}
