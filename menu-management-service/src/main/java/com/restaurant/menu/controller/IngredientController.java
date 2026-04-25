package com.restaurant.menu.controller;

import com.restaurant.menu.dto.BulkUpdateStockRequest;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.dto.UpdateStockRequest;
import com.restaurant.menu.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public ResponseEntity<List<IngredientDTO>> getAllIngredients() {
        return ResponseEntity.ok(ingredientService.getAllIngredients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientDTO> getIngredientById(@PathVariable Long id) {
        return ResponseEntity.ok(ingredientService.getIngredientById(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<IngredientDTO> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(ingredientService.updateStock(id, request.stock()));
    }

    @PatchMapping("/bulk/stock")
    public ResponseEntity<List<IngredientDTO>> bulkUpdateStock(
            @Valid @RequestBody BulkUpdateStockRequest request) {
        return ResponseEntity.ok(ingredientService.bulkUpdateStock(request.updates()));
    }
}
