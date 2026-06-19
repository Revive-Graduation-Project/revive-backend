package com.restaurant.auth.controller;

import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthResponse;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.dto.StaffSignupRequest;
import com.restaurant.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user and returns a JWT on success.
     * Never exposes the User entity; delegates entirely to AuthService.
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    /**
     * Registers a new staff member.
     * ADMIN can create CHEF and MANAGER roles.
     * MANAGER can only create CHEF role.
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @PostMapping("/staff/signup")
    public ResponseEntity<MessageResponse> signupStaff(@Valid @RequestBody StaffSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupStaff(request));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
