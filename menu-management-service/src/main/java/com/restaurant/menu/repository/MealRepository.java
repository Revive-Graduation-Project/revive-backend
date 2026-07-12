package com.restaurant.menu.repository;

import com.restaurant.menu.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {

    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    Optional<Meal> findByName(String name);

    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    List<Meal> findByHasDiscount(Boolean hasDiscount);
    
    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    List<Meal> findByIsActive(Boolean isActive);

    @Override
    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    List<Meal> findAll();

    @Override
    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    Optional<Meal> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"mealIngredients", "mealIngredients.ingredient"})
    List<Meal> findAllById(Iterable<Long> ids);
}
