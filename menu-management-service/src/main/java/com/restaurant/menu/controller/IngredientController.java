package com.restaurant.menu.controller;

import com.restaurant.menu.dto.BulkUpdateStockRequest;
import com.restaurant.menu.dto.IngredientDTO;
import com.restaurant.menu.dto.ReserveMealDTO;
import com.restaurant.menu.dto.UpdateStockRequest;
import com.restaurant.menu.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Slf4j
public class IngredientController {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER");

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
    public ResponseEntity<?> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role))
            return forbidden(role);
        return ResponseEntity.ok(ingredientService.updateStock(id, request.stock()));
    }

    @PatchMapping("/bulk/stock")
    public ResponseEntity<?> bulkUpdateStock(
            @Valid @RequestBody BulkUpdateStockRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role))
            return forbidden(role);
        return ResponseEntity.ok(ingredientService.bulkUpdateStock(request.updates()));
    }

    /**
     * Called by order-service when an order is placed.
     * Deducts ingredient stock for all ordered meals (pessimistic-locked).
     *
     * POST /api/ingredients/reserve
     * Body: [{"mealId": 1, "quantity": 2}, {"mealId": 3, "quantity": 1}]
     */
    @PostMapping("/reserve")
    public ResponseEntity<?> reserveStock(
            @Valid @RequestBody List<@Valid ReserveMealDTO> meals,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role))
            return forbidden(role);
        return ResponseEntity.ok(ingredientService.reserveIngredients(meals));
    }

    /**
     * Called by order-service when an order fails or is cancelled.
     * Adds the previously deducted stock back.
     *
     * POST /api/ingredients/revert
     * Body: [{"mealId": 1, "quantity": 2}, {"mealId": 3, "quantity": 1}]
     */
    @PostMapping("/revert")
    public ResponseEntity<?> revertStock(
            @Valid @RequestBody List<@Valid ReserveMealDTO> meals,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role))
            return forbidden(role);
        ingredientService.revertIngredients(meals);
        return ResponseEntity.status(HttpStatus.OK).body("Stock reverted successfully");
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private boolean isAuthorized(String role) {
        if (role == null) return true;
        String normalized = role.toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return ALLOWED_ROLES.contains(normalized);
    }

    private ResponseEntity<?> forbidden(String role) {
        log.warn("Unauthorized access attempt to /api/ingredients with role: {}", role);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access denied. Only ADMIN or MANAGER roles are allowed.");
    }
}
