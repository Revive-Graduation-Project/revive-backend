package com.restaurant.auth.service;

import com.restaurant.auth.config.SecurityUser;
import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // 64-byte Base64 secret key (valid for HS256/HS384/HS512)
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHkta2V5LW9ubHk=";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);

        testUser = User.builder()
                .id(1L)
                .email("testuser@example.com")
                .password("hashed-password")
                .role(Role.CLIENT)
                .isActive(true)
                .build();
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken() returns a non-blank JWT string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        // JWTs always have exactly two dots separating the three Base64url parts
        assertThat(token.chars().filter(c -> c == '.').count()).isEqualTo(2);
    }

    // ── extractEmail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractEmail() returns the email embedded as the JWT subject claim")
    void extractEmail_roundTrip() {
        String token = jwtService.generateToken(testUser);

        String extracted = jwtService.extractEmail(token);

        // getUsername() on User now returns the email field
        assertThat(extracted).isEqualTo(new SecurityUser(testUser).getUsername());
        assertThat(extracted).isEqualTo("testuser@example.com");
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid() returns true for a valid token matching the user")
    void isTokenValid_withMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(testUser);

        boolean valid = jwtService.isTokenValid(token, new SecurityUser(testUser));

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false when token belongs to a different user")
    void isTokenValid_withWrongUser_returnsFalse() {
        String token = jwtService.generateToken(testUser);

        UserDetails differentUser = new SecurityUser(User.builder()
                .id(2L)
                .email("other@example.com")
                .password("pw")
                .role(Role.CHEF)
                .isActive(true)
                .build());

        boolean valid = jwtService.isTokenValid(token, differentUser);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() returns false for an already-expired token")
    void isTokenValid_withExpiredToken_returnsFalse() {
        // Override service with 0ms expiry so the token expires instantly
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(expiredJwtService, "expirationMs", 0L);

        String expiredToken = expiredJwtService.generateToken(testUser);

        boolean valid = expiredJwtService.isTokenValid(expiredToken, new SecurityUser(testUser));

        assertThat(valid).isFalse();
    }
}
