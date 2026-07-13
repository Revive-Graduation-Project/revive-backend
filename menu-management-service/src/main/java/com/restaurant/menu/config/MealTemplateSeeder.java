package com.restaurant.menu.config;

import com.restaurant.menu.entity.MealSlot;
import com.restaurant.menu.entity.MealTemplate;
import com.restaurant.menu.repository.MealTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealTemplateSeeder implements CommandLineRunner {

    private final MealTemplateRepository mealTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Running MealTemplateSeeder...");

        // Always re-seed to ensure correct USDA categories are present
        mealTemplateRepository.deleteAll();

        // 1. Poultry Products Template
        MealTemplate poultryTemplate = MealTemplate.builder()
                .name("Custom Poultry Bowl")
                .primaryCategory("Poultry Products")
                .build();
        
        poultryTemplate.setSlots(List.of(
                MealSlot.builder().template(poultryTemplate).slotName("Base").ingredientCategory("Cereal Grains and Pasta").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(poultryTemplate).slotName("Bread").ingredientCategory("Baked Products").maxSelect(1).isRequired(false).build(),
                MealSlot.builder().template(poultryTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build(),
                MealSlot.builder().template(poultryTemplate).slotName("Cheese").ingredientCategory("Dairy and Egg Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(poultryTemplate).slotName("Sauce").ingredientCategory("Fats and Oils").maxSelect(2).isRequired(false).build()
        ));

        // 2. Beef Products Template
        MealTemplate beefTemplate = MealTemplate.builder()
                .name("Custom Beef Bowl")
                .primaryCategory("Beef Products")
                .build();
        
        beefTemplate.setSlots(List.of(
                MealSlot.builder().template(beefTemplate).slotName("Base").ingredientCategory("Cereal Grains and Pasta").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(beefTemplate).slotName("Bread").ingredientCategory("Baked Products").maxSelect(1).isRequired(false).build(),
                MealSlot.builder().template(beefTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build(),
                MealSlot.builder().template(beefTemplate).slotName("Cheese").ingredientCategory("Dairy and Egg Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(beefTemplate).slotName("Sauce").ingredientCategory("Fats and Oils").maxSelect(2).isRequired(false).build()
        ));

        // 3. Seafood Template
        MealTemplate fishTemplate = MealTemplate.builder()
                .name("Custom Seafood Bowl")
                .primaryCategory("Finfish and Shellfish Products")
                .build();
        
        fishTemplate.setSlots(List.of(
                MealSlot.builder().template(fishTemplate).slotName("Base").ingredientCategory("Cereal Grains and Pasta").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(fishTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build(),
                MealSlot.builder().template(fishTemplate).slotName("Sauce").ingredientCategory("Fats and Oils").maxSelect(2).isRequired(false).build()
        ));

        // 4. Legumes Template
        MealTemplate legumeTemplate = MealTemplate.builder()
                .name("Custom Vegan Bowl")
                .primaryCategory("Legumes and Legume Products")
                .build();
        
        legumeTemplate.setSlots(List.of(
                MealSlot.builder().template(legumeTemplate).slotName("Base").ingredientCategory("Cereal Grains and Pasta").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(legumeTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build(),
                MealSlot.builder().template(legumeTemplate).slotName("Sauce").ingredientCategory("Fats and Oils").maxSelect(2).isRequired(false).build()
        ));

        mealTemplateRepository.saveAll(List.of(poultryTemplate, beefTemplate, fishTemplate, legumeTemplate));
        
        log.info("MealTemplateSeeder finished. Created USDA-aligned standard templates.");
    }
}
