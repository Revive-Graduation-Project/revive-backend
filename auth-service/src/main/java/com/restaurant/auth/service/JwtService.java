package com.restaurant.auth.service;

import com.restaurant.auth.config.SecurityUser;
import com.restaurant.auth.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a signed JWT token for the given user.
     * Claims include: email (subject), id, role.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("role", user.getRole().name());
        return buildToken(extraClaims, new SecurityUser(user), expirationMs);
    }

    /**
     * Generates a signed JWT refresh token for the given user.
     */
    public String generateRefreshToken(User user, String familyId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");
        extraClaims.put("family", familyId);
        return buildToken(extraClaims, new SecurityUser(user), refreshExpirationMs, UUID.randomUUID().toString());
    }

    /**
     * Extracts the email (stored as the JWT subject claim) from the token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            // userDetails.getUsername() returns the email (see User.getUsername() override)
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            // An expired token is simply invalid — return false instead of propagating.
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("type", String.class);
            String tokenFamily = claims.get("family", String.class);
            String tokenJti = claims.getId();

            return "refresh".equals(tokenType)
                    && tokenJti != null
                    && tokenFamily != null
                    && user.getEmail().equals(claims.getSubject())
                    && tokenFamily.equals(user.getRefreshTokenFamily())
                    && !isTokenExpired(token);
        } catch (ExpiredJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return buildToken(extraClaims, userDetails, expiration, UUID.randomUUID().toString());
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration, String jti) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
