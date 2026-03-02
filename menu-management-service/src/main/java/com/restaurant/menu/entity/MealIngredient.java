package com.restaurant.menu.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Junction table linking a Meal to ingredients from the inventory-service.
 * Since ingredients live in a separate service/database, we store only the
 * UUID reference (ingredientId) instead of a real @ManyToMany JPA join.
 */
@Entity
@Table(name = "meal_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // Many MealIngredients belong to one Meal (within this DB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    // Cross-service reference to inventory-service's Ingredient (UUID only — no FK constraint)
    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    // Quantity of this ingredient used in the meal, in grams
    @Column(nullable = false)
    private Double quantityGrams;
}
