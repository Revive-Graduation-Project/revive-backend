package com.restaurant.inventory.controller;

import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.ValidationResult;
import com.restaurant.inventory.dto.ImportResponse;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.service.MenuCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {

    private final MenuCsvService menuCsvService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        // 1. Authorization Check: Only allow ADMIN users to upload menus
        if (!"ADMIN".equals(role)) {
            log.warn("Unauthorized upload attempt by role: {}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only ADMINs can upload the menu.");
        }

        // 2. Proceed with business logic
        log.info("Received menu CSV upload: '{}' by ADMIN", file.getOriginalFilename());
        List<MealNutrition> result = menuCsvService.getMenuNutritions(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Validating menu CSV: '{}'", file.getOriginalFilename());
        return ResponseEntity.ok(menuCsvService.validateCsv(file));
    }

    @PostMapping("/import-json")
    public ResponseEntity<ImportResponse> importJson(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody List<CsvParserHelper.MealCsvEntry> meals) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Received JSON import request for {} meals", meals.size());
        ImportResponse response = menuCsvService.importFromJson(meals);
        return ResponseEntity.accepted().body(response);  // 202 Accepted — async saga
    }
}