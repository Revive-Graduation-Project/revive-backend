package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.dto.UpdateDisplayNameRequest;
import com.restaurant.kitchen.dto.UpdateStationRequest;
import com.restaurant.kitchen.dto.UpdateStatusRequest;
import com.restaurant.kitchen.exception.ForbiddenRoleException;
import com.restaurant.kitchen.service.KitchenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kitchen/chefs")
public class AdminController {

    private final KitchenService service;

    private void validateAdminRole(String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw new ForbiddenRoleException();
        }
    }

    @PatchMapping("/{id}/display-name")
    public ResponseEntity<Void> updateDisplayName(@PathVariable Long id,
                                                  @RequestHeader("X-User-Role") String userRole,
                                                  @Valid @RequestBody UpdateDisplayNameRequest displayNameRequest) {
        validateAdminRole(userRole);
        service.updateDisplayName(id, displayNameRequest.displayName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/station")
    public ResponseEntity<Void> updateStation(@PathVariable Long id,
                                              @RequestHeader("X-User-Role") String userRole,
                                              @Valid @RequestBody UpdateStationRequest stationRequest) {

        validateAdminRole(userRole);
        service.updateStation(id, stationRequest.station());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @RequestHeader("X-User-Role") String userRole,
                                             @Valid @RequestBody UpdateStatusRequest statusRequest) {

        validateAdminRole(userRole);
        service.updateStatus(id, statusRequest.status());
        return ResponseEntity.ok().build();
    }
}
