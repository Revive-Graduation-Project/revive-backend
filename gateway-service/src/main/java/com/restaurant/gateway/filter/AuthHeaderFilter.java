package com.restaurant.gateway.filter;

import com.restaurant.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global gateway filter that acts as the single security enforcer for the
 * cluster.
 * By removing spring-boot-starter-security, we completely bypass the Reactor
 * StackOverflowError bugs caused by insanely deep WebFlux security chains.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Define routes that do not require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/signup",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/webjars/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // 1. Check if route is public
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith) ||
                path.contains("/v3/api-docs") ||
                (method.equals("GET") && (path.startsWith("/api/menu") || path.startsWith("/api/ingredients")));

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 2. If no token is provided
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isPublic) {
                return chain.filter(exchange); // Let public routes through
            } else {
                log.warn("Unauthorized access attempt to protected route: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete(); // Reject
            }
        }

        // 3. Token is provided, validate it
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.extractAllClaims(token);

            String userId = claims.get("id").toString();
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // Mutate the request to add trusted identity headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            log.debug("Forwarding request with X-User-Id={}, X-User-Role={}", userId, role);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            // If token is invalid, reject the request (even for public routes, to be safe)
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
