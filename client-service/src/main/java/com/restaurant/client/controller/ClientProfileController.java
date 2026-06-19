package com.restaurant.client.controller;

import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;
import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping
    public ResponseEntity<ClientProfileDto> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(clientProfileService.getProfile(userId));
    }

    @PutMapping
    public ResponseEntity<ClientProfileDto> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateClientProfileRequest request) {
        return ResponseEntity.ok(clientProfileService.updateProfile(userId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile(@RequestHeader("X-User-Id") Long userId) {
        clientProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }
}
