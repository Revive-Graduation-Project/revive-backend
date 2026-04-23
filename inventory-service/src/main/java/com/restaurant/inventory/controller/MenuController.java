package com.restaurant.inventory.controller;

import com.restaurant.inventory.dto.IngredientNutrition;
import com.restaurant.inventory.dto.MealNutrition;
import com.restaurant.inventory.service.MenuCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@Slf4j
@RequiredArgsConstructor
public class MenuController {

    private final MenuCsvService menuCsvService;

    @PostMapping("/upload")
    public ResponseEntity<List<MealNutrition>> uploadMenu(
            @RequestParam("file") MultipartFile file) {
        log.info("Received menu CSV upload: '{}'", file.getOriginalFilename());
        List<MealNutrition> result = menuCsvService.getMenuNutritions(file);
        return ResponseEntity.ok(result);
    }
}