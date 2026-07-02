package com.restaurant.auth.controller;

import com.restaurant.auth.dto.AdminSignupRequest;
import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthResponse;
import com.restaurant.auth.dto.AuthTokenPair;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.dto.StaffSignupRequest;
import com.restaurant.auth.service.AuthService;
import com.restaurant.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/staff/signup")
    public ResponseEntity<MessageResponse> signupStaff(@Valid @RequestBody StaffSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupStaff(request));
    }

    /**
     * Registers a new ADMIN member.
     * Only accessible to existing ADMIN roles.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/signup")
    public ResponseEntity<MessageResponse> signupAdmin(@Valid @RequestBody AdminSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupAdmin(request));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthTokenPair tokenPair = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken()).toString())
                .body(toPublicResponse(tokenPair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing");
        }

        AuthTokenPair tokenPair = authService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken()).toString())
                .body(toPublicResponse(tokenPair));
    }

    private AuthResponse toPublicResponse(AuthTokenPair tokenPair) {
        return new AuthResponse(
                tokenPair.token(),
                tokenPair.role(),
                tokenPair.userId(),
                tokenPair.emailString(),
                tokenPair.firstName(),
                tokenPair.lastName());
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(jwtService.getRefreshExpirationSeconds())
                .build();
    }
}
