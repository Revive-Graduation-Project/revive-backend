package com.restaurant.menu.repository;

import com.restaurant.menu.entity.Ingredient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByName(String name);

    List<Ingredient> findByCategory(String category);

    List<Ingredient> findByCategoryAndStockGreaterThan(String category, Double stock);

    /**
     * Fetches ingredients by their IDs with a PESSIMISTIC_WRITE lock
     * (translates to SELECT ... FOR UPDATE in PostgreSQL).
     * Blocks concurrent transactions from reading/writing the same rows
     * until this transaction commits — preventing double-deduction of stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Ingredient i WHERE i.id IN :ids")
    List<Ingredient> findAllByIdWithLock(List<Long> ids);
}
