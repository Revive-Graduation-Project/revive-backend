package com.restaurant.inventory.controller;

import com.restaurant.inventory.dto.ImportJobDto;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.dto.ValidationResult;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.service.ImportJobService;
import com.restaurant.inventory.service.MenuCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {

    private final MenuCsvService menuCsvService;
    private final ImportJobService importJobService;

    private static final String ROLE_ADMIN   = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";

    // ── Auth helpers ──────────────────────────────────────────────────
    private boolean isAdminOrManager(String role) {
        return ROLE_ADMIN.equals(role) || ROLE_MANAGER.equals(role);
    }

    // ── Legacy raw-upload (kept for backward compat) ──────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        if (!ROLE_ADMIN.equals(role)) {
            log.warn("Unauthorized upload attempt by role: {}", role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMINs can upload the menu.");
        }
        log.info("Received menu CSV upload: '{}' by ADMIN", file.getOriginalFilename());
        List<MealNutrition> result = menuCsvService.getMenuNutritions(file);
        return ResponseEntity.ok(result);
    }

    // ── Validate CSV ──────────────────────────────────────────────────
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateMenu(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("file") MultipartFile file) {

        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Validating menu CSV: '{}'", file.getOriginalFilename());
        return ResponseEntity.ok(menuCsvService.validateCsv(file));
    }

    // ── Start async import ────────────────────────────────────────────
    @PostMapping("/import-json")
    public ResponseEntity<Map<String, Object>> importJson(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody List<CsvParserHelper.MealCsvEntry> meals) {

        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Map<String, Object> response = importJobService.startImport(meals);
        log.info("Import started — jobId={}, meals={}", response.get("jobId"), meals.size());
        return ResponseEntity.accepted().body(response);
    }

    // ── Get status of a specific job ──────────────────────────────────
    @GetMapping("/import-status/{jobId}")
    public ResponseEntity<ImportJobDto> getImportStatus(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String jobId) {
        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return importJobService.getJobStatus(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Get full history ──────────────────────────────────────────────
    @GetMapping("/import-jobs")
    public ResponseEntity<org.springframework.data.domain.Page<ImportJobDto>> getAllImportJobs(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(importJobService.getAllJobs(pageable));
    }

    // ── Get currently active job (with heartbeat timeout check) ───────
    @GetMapping("/import-jobs/active")
    public ResponseEntity<ImportJobDto> getActiveImportJob(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return importJobService.getActiveJob()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── Cancel a job ──────────────────────────────────────────────────
    @PostMapping("/import-jobs/{jobId}/cancel")
    public ResponseEntity<ImportJobDto> cancelImportJob(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String jobId) {

        if (!isAdminOrManager(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return importJobService.cancelJob(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}