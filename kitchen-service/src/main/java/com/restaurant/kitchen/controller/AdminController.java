package com.restaurant.kitchen.controller;

import com.restaurant.kitchen.dto.ChefProfileDTO;
import com.restaurant.kitchen.dto.UpdateDisplayNameRequest;
import com.restaurant.kitchen.dto.UpdateStationRequest;
import com.restaurant.kitchen.dto.UpdateChefStatusRequest;
import com.restaurant.kitchen.service.ChefService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kitchen/chefs")
public class AdminController {

    private final ChefService service;


    @PatchMapping("/{id}/display-name")
    public ResponseEntity<ChefProfileDTO> updateDisplayName(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateDisplayNameRequest displayNameRequest) {

        ChefProfileDTO updatedChefProfile = service.updateChefDisplayName(id, displayNameRequest.displayName());
        return ResponseEntity.ok(updatedChefProfile);
    }

    @PatchMapping("/{id}/station")
    public ResponseEntity<ChefProfileDTO> updateStation(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStationRequest stationRequest) {

        ChefProfileDTO updatedChefProfile = service.updateChefStation(id, stationRequest.station());
        return ResponseEntity.ok(updatedChefProfile);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ChefProfileDTO> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateChefStatusRequest statusRequest) {

        ChefProfileDTO updatedChefProfile = service.updateChefStatus(id, statusRequest.status());
        return ResponseEntity.ok(updatedChefProfile);
    }
}
