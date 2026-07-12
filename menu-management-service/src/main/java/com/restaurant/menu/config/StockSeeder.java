package com.restaurant.menu.config;

import com.restaurant.menu.entity.Ingredient;
import com.restaurant.menu.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSeeder implements CommandLineRunner {

    private final IngredientRepository ingredientRepository;

    @Override
    public void run(String... args) {
        log.info("Running StockSeeder...");
        List<Ingredient> ingredients = ingredientRepository.findAll();
        int updated = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.getStock() == null || ingredient.getStock() <= 0.0) {
                ingredient.setStock(2000.0);
                ingredientRepository.save(ingredient);
                updated++;
            }
        }
        log.info("StockSeeder finished. Updated {} ingredients.", updated);
    }
}
