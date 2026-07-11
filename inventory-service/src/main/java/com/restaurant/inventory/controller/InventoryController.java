package com.restaurant.inventory.controller;

import com.restaurant.inventory.dto.ImportJobStatus;
import com.restaurant.inventory.dto.ImportResponse;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.ValidationResult;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.service.ImportJobStore;
import com.restaurant.inventory.service.MenuCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ImportJobStore importJobStore;

    // ── Auth guard ────────────────────────────────────────────────────────────

    /**
     * Verifies that the caller is either ADMIN or MANAGER.
     * Returns a 403 response entity if not; null if allowed.
     */
    private ResponseEntity<?> assertAllowed(String role) {
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) return null;
        log.warn("Forbidden request from role: {}", role);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Only ADMINs and MANAGERs can perform this action.");
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        ResponseEntity<?> forbidden = assertAllowed(role);
        if (forbidden != null) return forbidden;

        log.info("Received menu CSV upload: '{}' by {}", file.getOriginalFilename(), role);
        List<MealNutrition> result = menuCsvService.getMenuNutritions(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        ResponseEntity<?> forbidden = assertAllowed(role);
        if (forbidden != null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("Validating menu CSV: '{}'", file.getOriginalFilename());
        return ResponseEntity.ok(menuCsvService.validateCsv(file));
    }

    @PostMapping("/import-json")
    public ResponseEntity<ImportResponse> importJson(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody List<CsvParserHelper.MealCsvEntry> meals) {

        ResponseEntity<?> forbidden = assertAllowed(role);
        if (forbidden != null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        log.info("Received JSON import request for {} meals by {}", meals.size(), role);
        ImportResponse response = menuCsvService.importFromJson(meals);
        return ResponseEntity.accepted().body(response);  // 202 Accepted — async pipeline
    }

    @GetMapping("/import-status/{jobId}")
    public ResponseEntity<ImportJobStatus> importStatus(
            @PathVariable String jobId) {

        return importJobStore.getStatus(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}