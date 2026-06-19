package com.restaurant.auth.config;

import com.restaurant.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ApplicationConfig applicationConfig;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
            ApplicationConfig applicationConfig) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.applicationConfig = applicationConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — no HTTP session created or used
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorisation rules
                .authorizeHttpRequests(auth -> auth
                        // 1. Your public auth endpoints
                        .requestMatchers("/auth/signup", "/auth/login").permitAll()

                        // 2. Swagger UI and OpenAPI docs (Safe for public viewing)
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/auth/v3/api-docs",
                                "/auth/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html")
                        .permitAll()

                        // 3. Actuator Health Check ONLY (Crucial for Docker, safe to expose)
                        .requestMatchers("/actuator/health").permitAll()

                        // 4. Everything else requires a valid JWT
                        .anyRequest().authenticated())

                // Wire our DaoAuthenticationProvider
                .authenticationProvider(applicationConfig.authenticationProvider())

                // JWT filter runs before Spring's username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/v3/api-docs",
                "/auth/v3/api-docs",
                "/auth/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-ui/index.html");
    }

    // ── JWT Authentication Filter (inner-component kept alongside its config) ─

    @Component
    @RequiredArgsConstructor
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final UserDetailsService userDetailsService;

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain)
                throws ServletException, IOException {

            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            // Skip filter if no Bearer token present
            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);
            final String email;

            try {
                email = jwtService.extractEmail(jwt);
            } catch (Exception e) {
                // Malformed / invalid token — let the request fall through to 403
                filterChain.doFilter(request, response);
                return;
            }

            // Only authenticate if not already authenticated
            if (StringUtils.hasText(email)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // loadUserByUsername() interface parameter is called 'username',
                // but here we pass the email — our UserDetailsService queries by email.
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
