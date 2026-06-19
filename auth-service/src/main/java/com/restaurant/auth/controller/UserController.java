package com.restaurant.auth.controller;

import com.restaurant.auth.dto.UserResponse;
import com.restaurant.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns all users in the system.
     * Only accessible to ADMIN.
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Returns only staff members (CHEF and MANAGER).
     * Accessible to MANAGER.
     */
    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/staff")
    public ResponseEntity<List<UserResponse>> getStaffUsers() {
        return ResponseEntity.ok(userService.getStaffUsers());
    }
}
