package com.restaurant.inventory.controller;

import com.restaurant.inventory.entity.Ingredient;
import com.restaurant.inventory.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final InventoryService inventoryService;

    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getIngredient(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getIngredientById(id));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@RequestBody com.restaurant.inventory.dto.ReservationRequest request) {
        inventoryService.reserveStock(request.items());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/commit")
    public ResponseEntity<Void> commit(@RequestBody com.restaurant.inventory.dto.ReservationRequest request) {
        inventoryService.commitStock(request.items());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rollback")
    public ResponseEntity<Void> rollback(@RequestBody com.restaurant.inventory.dto.ReservationRequest request) {
        inventoryService.rollbackStock(request.items());
        return ResponseEntity.ok().build();
    }
}
