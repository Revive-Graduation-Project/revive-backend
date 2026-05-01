package com.restaurant.menu.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Explicit join entity for the meal_ingredients table.
 * Replaces the implicit @ManyToMany so we can store the quantity (in grams)
 * of each ingredient used per serving of the meal.
 */
@Entity
@Table(name = "meal_ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /**
     * How many grams of this ingredient are used per serving of the meal.
     * e.g. a burger uses 150.0g of beef.
     */
    @Column(name = "quantity_grams", nullable = false)
    private Double quantityGrams;
}
