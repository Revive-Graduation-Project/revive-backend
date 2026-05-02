package com.restaurant.menu.controller;

import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealRequest;
import com.restaurant.menu.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER");

    private final MealService mealService;

    @GetMapping
    public ResponseEntity<List<MealDTO>> getAllMeals() {
        return ResponseEntity.ok(mealService.getAllMeals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealDTO> getMealById(@PathVariable Long id) {
        return ResponseEntity.ok(mealService.getMealById(id));
    }

    @PostMapping
    public ResponseEntity<?> createMeal(
            @Valid @RequestBody MealRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        MealDTO created = mealService.createMeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeal(
            @PathVariable Long id,
            @Valid @RequestBody MealRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        MealDTO updated = mealService.updateMeal(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeal(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        mealService.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private boolean isAuthorized(String role) {
        return role != null && ALLOWED_ROLES.contains(role.toUpperCase());
    }

    private ResponseEntity<?> forbidden(String role) {
        log.warn("Unauthorized access attempt to /api/menu with role: {}", role);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access denied. Only ADMIN or MANAGER roles are allowed.");
    }
}
