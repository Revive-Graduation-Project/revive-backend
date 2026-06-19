package com.restaurant.auth.service;

import com.restaurant.auth.config.RabbitMQConfig;
import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthResponse;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.exception.EmailAlreadyExistsException;
import com.restaurant.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("$2a$hashed")
                .role(Role.CLIENT)
                .isActive(true)
                .build();
    }

    // ── signup ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("signup() returns a message and publishes a UserCreatedEvent on success")
    void signup_success_returnsMessage() {
        SignupRequest request = new SignupRequest("john@example.com", "password123", null, null, null, null, null, null, null, null, null, null);

        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$hashed");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        MessageResponse response = authService.signup(request);

        assertThat(response.message()).isEqualTo("User registered successfully. Profile creation is pending.");

        // Verify the user was persisted with the correct email and hashed password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$hashed");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CLIENT);

        // Verify RabbitMQ publish (any(Object.class) disambiguates the overload)
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_CREATED),
                any(Object.class));
    }

    @Test
    @DisplayName("signup() throws EmailAlreadyExistsException when email is taken")
    void signup_duplicateEmail_throwsException() {
        SignupRequest request = new SignupRequest("john@example.com", "password123", null, null, null, null, null, null, null, null, null, null);
        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() returns a token when credentials are valid")
    void login_success_returnsToken() {
        AuthRequest request = new AuthRequest("john@example.com", "password123");

        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));
        given(jwtService.generateToken(savedUser)).willReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123"));
    }

    @Test
    @DisplayName("login() propagates BadCredentialsException when credentials are invalid")
    void login_badCredentials_throws() {
        AuthRequest request = new AuthRequest("john@example.com", "wrong-password");

        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }
}
