package com.restaurant.client.controller;

import com.restaurant.client.dto.ClientProfileDto;
import com.restaurant.client.dto.UpdateClientProfileRequest;
import com.restaurant.client.service.ClientProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clients/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping
    public ResponseEntity<?> getAllProfiles(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Only ADMIN role is allowed.");
        }
        return ResponseEntity.ok(clientProfileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientProfileDto> getProfile(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(clientProfileService.getProfile(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientProfileDto> updateProfile(@PathVariable("id") Long userId,
            @RequestBody UpdateClientProfileRequest request) {
        return ResponseEntity.ok(clientProfileService.updateProfile(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable("id") Long userId) {
        clientProfileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable("id") Long userId,
            @RequestParam("file") MultipartFile file) {
        String pictureUrl = clientProfileService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(java.util.Map.of("profilePictureUrl", pictureUrl));
    }

    @DeleteMapping("/{id}/picture")
    public ResponseEntity<Void> deleteProfilePicture(@PathVariable("id") Long userId) {
        clientProfileService.deleteProfilePicture(userId);
        return ResponseEntity.noContent().build();
    }
}
