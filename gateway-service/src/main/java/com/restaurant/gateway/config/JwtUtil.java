package com.restaurant.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Stateless JWT utility that mirrors the auth-service's signing logic.
 * Uses the exact same HMAC-SHA secret so tokens created by auth-service
 * can be validated here without a JWKS endpoint.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parses and validates the token. Returns the full Claims on success,
     * or throws (ExpiredJwtException, SignatureException, etc.) on failure.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Convenience — subject is the user's email in our tokens. */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Convenience — "id" claim holds the user's database PK. */
    public String extractUserId(String token) {
        return extractAllClaims(token).get("id").toString();
    }

    /** Convenience — "role" claim holds the role name (e.g. ADMIN). */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}
