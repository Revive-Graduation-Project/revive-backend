package com.restaurant.order.client;

import com.restaurant.order.dto.snapshot.CustomIngredientSnapshot;

import java.util.Map;
import java.util.UUID;

public interface InventoryClient {
    CustomIngredientSnapshot getIngredientById(UUID id);
    
    void reserve(Map<UUID, Double> items);
    
    void commit(Map<UUID, Double> items);
    
    void rollback(Map<UUID, Double> items);
}
