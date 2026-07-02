package com.restaurant.auth.service;

import com.restaurant.auth.config.RabbitMQConfig;
import com.restaurant.auth.config.SecurityUser;
import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import com.restaurant.auth.dto.AdminSignupRequest;
import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthTokenPair;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.event.UserCreatedEvent;
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
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
                .firstName("John")
                .lastName("Doe")
                .role(Role.CLIENT)
                .isActive(true)
                .build();
    }

    // ── signup ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("signup() returns a message and publishes a UserCreatedEvent on success")
    void signup_success_returnsMessage() {
        SignupRequest request = new SignupRequest("john@example.com", "password123", "John", "Doe", null, null, null, null, null, null, null, null, null, null);

        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$hashed");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        MessageResponse response = authService.signup(request);

        assertThat(response.message()).isEqualTo("User registered successfully. Profile creation is pending.");

        // Verify the user was persisted with the correct email, hashed password, and names
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$hashed");
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.CLIENT);

        // Verify RabbitMQ publish (any(Object.class) disambiguates the overload)
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_CLIENT_CREATED),
                any(Object.class));
    }

    @Test
    @DisplayName("publishUserCreatedEvent() swallows RabbitMQ failures")
    void publishUserCreatedEvent_rabbitFailure_doesNotThrow() {
        doThrow(new AmqpException("Broker unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        UserCreatedEvent event = UserCreatedEvent.builder().id(savedUser.getId()).role("CLIENT").build();

        assertThatCode(() -> authService.publishUserCreatedEvent(savedUser, event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("signup() throws EmailAlreadyExistsException when email is taken")
    void signup_duplicateEmail_throwsException() {
        SignupRequest request = new SignupRequest("john@example.com", "password123", "John", "Doe", null, null, null, null, null, null, null, null, null, null);
        given(userRepository.existsByEmail("john@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ── signupAdmin ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("signupAdmin() returns a message and persists ADMIN role")
    void signupAdmin_success_returnsMessage() {
        AdminSignupRequest request = new AdminSignupRequest("admin@example.com", "password123", "Admin", "User");

        given(userRepository.existsByEmail("admin@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("$2a$hashed");
        
        User savedAdmin = User.builder()
                .id(2L)
                .email("admin@example.com")
                .password("$2a$hashed")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
                
        given(userRepository.save(any(User.class))).willReturn(savedAdmin);

        MessageResponse response = authService.signupAdmin(request);

        assertThat(response.message()).isEqualTo("Admin registered successfully.");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$hashed");
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Admin");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("User");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(userCaptor.getValue().getIsActive()).isTrue();
    }

    @Test
    @DisplayName("signupAdmin() throws EmailAlreadyExistsException when email is taken")
    void signupAdmin_duplicateEmail_throwsException() {
        AdminSignupRequest request = new AdminSignupRequest("admin@example.com", "password123", "Admin", "User");
        given(userRepository.existsByEmail("admin@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signupAdmin(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("admin@example.com");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() returns a token when credentials are valid")
    void login_success_returnsToken() {
        AuthRequest request = new AuthRequest("john@example.com", "password123");

        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));
        given(jwtService.generateToken(savedUser)).willReturn("jwt-token");
        given(jwtService.generateRefreshToken(eq(savedUser), anyString())).willReturn("refresh-token");

        AuthTokenPair response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(jwtService).generateRefreshToken(eq(savedUser), anyString());
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("john@example.com", "password123"));
    }

    @Test
    @DisplayName("refreshToken() returns a new auth response when the refresh token is valid")
    void refreshToken_validRefresh_returnsNewToken() {
        savedUser.setRefreshTokenFamily("family-1");
        savedUser.setRefreshTokenHash("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120");
        given(jwtService.extractEmail("refresh-token")).willReturn("john@example.com");
        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));
        given(jwtService.isRefreshTokenValid(eq("refresh-token"), any(User.class))).willReturn(true);
        given(jwtService.generateToken(savedUser)).willReturn("new-jwt-token");
        given(jwtService.generateRefreshToken(eq(savedUser), anyString())).willReturn("new-refresh-token");

        AuthTokenPair response = authService.refreshToken("refresh-token");

        assertThat(response.token()).isEqualTo("new-jwt-token");
        verify(jwtService).generateRefreshToken(eq(savedUser), anyString());
    }

    @Test
    @DisplayName("refreshToken() throws IllegalArgumentException for invalid refresh token")
    void refreshToken_invalidRefresh_throws() {
        given(jwtService.extractEmail("bad-token")).willThrow(new IllegalArgumentException("Malformed token"));

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("login() rejects inactive users")
    void login_inactiveUser_throws() {
        AuthRequest request = new AuthRequest("john@example.com", "password123");
        savedUser.setIsActive(false);

        given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(savedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is inactive");

        verify(jwtService, never()).generateRefreshToken(any(User.class), anyString());
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
