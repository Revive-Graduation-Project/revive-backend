package com.restaurant.menu.repository;

import com.restaurant.menu.entity.MealTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MealTemplateRepository extends JpaRepository<MealTemplate, Long> {
    Optional<MealTemplate> findByPrimaryCategory(String primaryCategory);
}
