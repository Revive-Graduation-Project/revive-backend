package com.restaurant.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthResponse;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.exception.GlobalExceptionHandler;
import com.restaurant.auth.exception.EmailAlreadyExistsException;
import com.restaurant.auth.service.AuthService;
import com.restaurant.auth.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters — we test controller logic, not security rules
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Web-Layer Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        // Required to satisfy the JwtAuthenticationFilter bean in the security config
        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        // ── POST /auth/signup ─────────────────────────────────────────────────────

        @Test
        @DisplayName("POST /auth/signup returns 201 and a message for a valid request")
        void signup_validRequest_returns201() throws Exception {
                SignupRequest request = new SignupRequest("johndoe@example.com", "secret123", null, null, null, null,
                                null, null, null, null, null, null);
                MessageResponse response = new MessageResponse("User registered successfully. Profile creation is pending.");

                given(authService.signup(request)).willReturn(response);

                mockMvc.perform(post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("User registered successfully. Profile creation is pending."));
        }

        @Test
        @DisplayName("POST /auth/signup returns 400 when username is blank")
        void signup_blankUsername_returns400() throws Exception {
                SignupRequest request = new SignupRequest("", "secret123", null, null, null, null, null, null, null,
                                null, null, null); // blank email triggers
                // @NotBlank

                mockMvc.perform(post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("POST /auth/signup returns 400 when password is too short")
        void signup_shortPassword_returns400() throws Exception {
                SignupRequest request = new SignupRequest("johndoe@example.com", "abc", null, null, null, null, null,
                                null, null, null, null, null);

                mockMvc.perform(post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("POST /auth/signup returns 400 when role is null")
        void signup_nullRole_returns400() throws Exception {
                // Build the JSON manually so we can emit `"role": null`
                String body = """
                                {
                                  "email": "johndoe@example.com",
                                  "password": "secret123",
                                  "role": null
                                }
                                """;

                mockMvc.perform(post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.role").exists()); // role is null → @NotNull fires
        }

        @Test
        @DisplayName("POST /auth/signup returns 409 when username is already taken")
        void signup_duplicateUsername_returns409() throws Exception {
                SignupRequest request = new SignupRequest("johndoe@example.com", "secret123", null, null, null, null,
                                null, null, null, null, null, null);

                given(authService.signup(request))
                                .willThrow(new EmailAlreadyExistsException("johndoe@example.com"));

                mockMvc.perform(post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message")
                                                .value("Email already registered: johndoe@example.com"));
        }

        // ── POST /auth/login ──────────────────────────────────────────────────────

        @Test
        @DisplayName("POST /auth/login returns 200 and a token for valid credentials")
        void login_validRequest_returns200() throws Exception {
                AuthRequest request = new AuthRequest("johndoe@example.com", "secret123");
                AuthResponse response = new AuthResponse("jwt-token-xyz");

                given(authService.login(request)).willReturn(response);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));
        }

        @Test
        @DisplayName("POST /auth/login returns 400 when username or password is blank")
        void login_blankFields_returns400() throws Exception {
                AuthRequest request = new AuthRequest("", ""); // blank email + password → both @NotBlank fire

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors").exists());
        }

        @Test
        @DisplayName("POST /auth/login returns 401 for invalid credentials")
        void login_badCredentials_returns401() throws Exception {
                AuthRequest request = new AuthRequest("johndoe@example.com", "wrong-password");

                given(authService.login(request))
                                .willThrow(new BadCredentialsException("Bad credentials"));

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
}
