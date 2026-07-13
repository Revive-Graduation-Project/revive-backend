package com.restaurant.menu.config;

import com.restaurant.menu.entity.MealSlot;
import com.restaurant.menu.entity.MealTemplate;
import com.restaurant.menu.repository.MealTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealTemplateSeeder implements CommandLineRunner {

    private final MealTemplateRepository mealTemplateRepository;

    @Override
    public void run(String... args) {
        log.info("Running MealTemplateSeeder...");

        if (mealTemplateRepository.count() > 0) {
            log.info("Meal templates already exist. Skipping seeding.");
            return;
        }

        MealTemplate burgerTemplate = MealTemplate.builder()
                .name("Custom Burger")
                .primaryCategory("Burgers")
                .build();
        
        burgerTemplate.setSlots(List.of(
                MealSlot.builder().template(burgerTemplate).slotName("Bun").ingredientCategory("Baked Products").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(burgerTemplate).slotName("Protein").ingredientCategory("Poultry Products").maxSelect(2).isRequired(true).build(),
                MealSlot.builder().template(burgerTemplate).slotName("Protein (Beef)").ingredientCategory("Beef Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(burgerTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build(),
                MealSlot.builder().template(burgerTemplate).slotName("Cheese").ingredientCategory("Dairy and Egg Products").maxSelect(2).isRequired(false).build()
        ));

        MealTemplate grillTemplate = MealTemplate.builder()
                .name("Custom Grill")
                .primaryCategory("Grills")
                .build();
        
        grillTemplate.setSlots(List.of(
                MealSlot.builder().template(grillTemplate).slotName("Protein").ingredientCategory("Poultry Products").maxSelect(2).isRequired(true).build(),
                MealSlot.builder().template(grillTemplate).slotName("Protein (Beef)").ingredientCategory("Beef Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(grillTemplate).slotName("Side").ingredientCategory("Cereal Grains and Pasta").maxSelect(1).isRequired(true).build(),
                MealSlot.builder().template(grillTemplate).slotName("Veggies").ingredientCategory("Vegetables and Vegetable Products").maxSelect(4).isRequired(false).build()
        ));

        MealTemplate saladTemplate = MealTemplate.builder()
                .name("Custom Salad")
                .primaryCategory("Salads")
                .build();
        
        saladTemplate.setSlots(List.of(
                MealSlot.builder().template(saladTemplate).slotName("Greens").ingredientCategory("Vegetables and Vegetable Products").maxSelect(2).isRequired(true).build(),
                MealSlot.builder().template(saladTemplate).slotName("Protein").ingredientCategory("Poultry Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(saladTemplate).slotName("Cheese").ingredientCategory("Dairy and Egg Products").maxSelect(2).isRequired(false).build(),
                MealSlot.builder().template(saladTemplate).slotName("Dressing").ingredientCategory("Fats and Oils").maxSelect(1).isRequired(false).build()
        ));

        mealTemplateRepository.saveAll(List.of(burgerTemplate, grillTemplate, saladTemplate));
        
        log.info("MealTemplateSeeder finished. Created standard templates.");
    }
}
