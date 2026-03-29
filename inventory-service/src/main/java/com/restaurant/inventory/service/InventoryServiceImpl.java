package com.restaurant.inventory.service;

import com.restaurant.inventory.entity.Ingredient;
import com.restaurant.inventory.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final IngredientRepository ingredientRepository;

    @Override
    public Ingredient getIngredientById(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found: " + id));
    }

    @Override
    @Transactional
    public void reserveStock(Map<UUID, Double> items) {
        for (Map.Entry<UUID, Double> entry : items.entrySet()) {
            Ingredient ingredient = getIngredientById(entry.getKey());
            double available = ingredient.getStockGrams() - ingredient.getReservedStockGrams();
            
            if (available < entry.getValue()) {
                throw new RuntimeException("Insufficient stock for " + ingredient.getName() + 
                    ". Available: " + available + ", Requested: " + entry.getValue());
            }
            
            ingredient.setReservedStockGrams(ingredient.getReservedStockGrams() + entry.getValue());
            ingredientRepository.save(ingredient);
        }
    }

    @Override
    @Transactional
    public void commitStock(Map<UUID, Double> items) {
        for (Map.Entry<UUID, Double> entry : items.entrySet()) {
            Ingredient ingredient = getIngredientById(entry.getKey());
            
            ingredient.setStockGrams(ingredient.getStockGrams() - entry.getValue());
            ingredient.setReservedStockGrams(ingredient.getReservedStockGrams() - entry.getValue());
            ingredientRepository.save(ingredient);
        }
    }

    @Override
    @Transactional
    public void rollbackStock(Map<UUID, Double> items) {
        for (Map.Entry<UUID, Double> entry : items.entrySet()) {
            Ingredient ingredient = getIngredientById(entry.getKey());
            ingredient.setReservedStockGrams(ingredient.getReservedStockGrams() - entry.getValue());
            ingredientRepository.save(ingredient);
        }
    }
}
