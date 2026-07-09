package com.restaurant.menu.controller;

import com.restaurant.menu.dto.BuildOptionsResponse;
import com.restaurant.menu.service.CustomizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customizations")
@RequiredArgsConstructor
public class CustomizationController {

    private final CustomizationService customizationService;

    @GetMapping("/build-options")
    public ResponseEntity<BuildOptionsResponse> getBuildOptions(@RequestParam String primaryCategory) {
        return ResponseEntity.ok(customizationService.getBuildOptions(primaryCategory));
    }
}
