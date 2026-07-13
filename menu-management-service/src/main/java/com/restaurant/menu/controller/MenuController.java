package com.restaurant.menu.controller;

import com.restaurant.menu.dto.DiscountRequest;
import com.restaurant.menu.dto.MealDTO;
import com.restaurant.menu.dto.MealRequest;
import com.restaurant.menu.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<List<MealDTO>> getAllMeals(
            @RequestParam(value = "hasDiscount", required = false) Boolean hasDiscount) {
        if (hasDiscount != null) {
            return ResponseEntity.ok(mealService.getMealsByDiscount(hasDiscount));
        }
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

    @PatchMapping("/{id}/discount")
    public ResponseEntity<?> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        MealDTO updated = mealService.updateDiscount(id, request);
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

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMealImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        String imageUrl = mealService.uploadMealImage(id, file);
        return ResponseEntity.ok(java.util.Map.of("imageUrl", imageUrl));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<?> deleteMealImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        mealService.deleteMealImage(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/bulk-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBulkMealImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAuthorized(role)) {
            return forbidden(role);
        }
        List<String> imageUrls = mealService.uploadBulkMealImages(files);
        return ResponseEntity.ok(java.util.Map.of("uploadedCount", imageUrls.size(), "urls", imageUrls));
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
        log.warn("Unauthorized access attempt to /api/menu with role: {}", role);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Access denied. Only ADMIN or MANAGER roles are allowed.");
    }
}
