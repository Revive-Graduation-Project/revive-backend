package com.restaurant.client.controller;

import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;
import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping
    public ResponseEntity<List<ClientProfileDto>> getAllProfiles() {
        return ResponseEntity.ok(clientProfileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientProfileDto> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(clientProfileService.getProfile(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientProfileDto> updateProfile(@PathVariable Long userId,
            @RequestBody UpdateClientProfileRequest request) {
        return ResponseEntity.ok(clientProfileService.updateProfile(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long userId) {
        clientProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
