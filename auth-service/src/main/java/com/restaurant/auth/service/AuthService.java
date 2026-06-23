package com.restaurant.auth.service;

import com.restaurant.auth.config.RabbitMQConfig;
import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import com.restaurant.auth.dto.AuthRequest;
import com.restaurant.auth.dto.AuthResponse;
import com.restaurant.auth.dto.MessageResponse;
import com.restaurant.auth.dto.SignupRequest;
import com.restaurant.auth.dto.StaffSignupRequest;
import com.restaurant.auth.event.UserCreatedEvent;
import com.restaurant.auth.exception.EmailAlreadyExistsException;
import com.restaurant.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final RabbitTemplate rabbitTemplate;

        // ── Signup ────────────────────────────────────────────────────────────────

        @Transactional
        public MessageResponse signup(SignupRequest request) {
                // 1. Guard: reject duplicate emails
                if (userRepository.existsByEmail(request.email())) {
                        throw new EmailAlreadyExistsException(request.email());
                }

                // 2. Persist the new user with a hashed password
                User user = User.builder()
                                .email(request.email())
                                .password(passwordEncoder.encode(request.password()))
                                .firstName(request.firstName())
                                .lastName(request.lastName())
                                .role(Role.CLIENT)
                                .isActive(true)
                                .build();

                user = userRepository.save(user);
                log.info("User created: id={}, role={}", user.getId(), user.getRole());

                // 3. Publish UserCreatedEvent to the shared exchange
                UserCreatedEvent event = UserCreatedEvent.builder()
                                .id(user.getId())
                                .role(user.getRole().name())
                                .phoneNumber(request.phoneNumber())
                                .age(request.age())
                                .gender(request.gender())
                                .exercisesRegularly(request.exercisesRegularly())
                                .height(request.height())
                                .heightUnit(request.heightUnit())
                                .weight(request.weight())
                                .weightUnit(request.weightUnit())
                                .goal(request.goal())
                                .healthConditions(request.healthConditions())
                                .build();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                rabbitTemplate.convertAndSend(
                                                RabbitMQConfig.EXCHANGE_NAME,
                                                RabbitMQConfig.ROUTING_KEY_CLIENT_CREATED,
                                                event);
                                log.info("Published UserCreatedEvent for userId={}", user.getId());
                        }
                });

                // 4. Return a success message (JWT will be acquired via login later)
                return new MessageResponse("User registered successfully. Profile creation is pending.");
        }

        @Transactional
        public MessageResponse signupStaff(StaffSignupRequest request) {
                // 1. Guard: never allow creating ADMIN or CLIENT via this endpoint
                if (request.role() == Role.ADMIN || request.role() == Role.CLIENT) {
                        throw new IllegalArgumentException(
                                        "Staff signup only allows CHEF or MANAGER roles");
                }

                // 2. Guard: enforce caller-specific permissions
                // ADMIN → can create CHEF, MANAGER
                // MANAGER → can only create CHEF
                String callerRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                                .iterator().next().getAuthority();

                if ("MANAGER".equals(callerRole) && request.role() != Role.CHEF) {
                        throw new IllegalArgumentException(
                                        "Managers can only create CHEF accounts");
                }

                // 3. Guard: reject duplicate emails
                if (userRepository.existsByEmail(request.email())) {
                        throw new EmailAlreadyExistsException(request.email());
                }

                // 4. Persist the new staff user with a hashed password
                User user = User.builder()
                                .email(request.email())
                                .password(passwordEncoder.encode(request.password()))
                                .firstName(request.firstName())
                                .lastName(request.lastName())
                                .role(request.role())
                                .isActive(true)
                                .build();

                user = userRepository.save(user);
                log.info("Staff user created: id={}, role={} (by {})", user.getId(), user.getRole(), callerRole);

                // 5. Publish UserCreatedEvent to the shared exchange only if it's a CHEF role
                if (user.getRole() == Role.CHEF) {
                        UserCreatedEvent event = UserCreatedEvent.builder()
                                        .id(user.getId())
                                        .role(user.getRole().name())
                                        .firstName(user.getFirstName())
                                        .lastName(user.getLastName())
                                        .build();
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                        rabbitTemplate.convertAndSend(
                                                        RabbitMQConfig.EXCHANGE_NAME,
                                                        RabbitMQConfig.ROUTING_KEY_CHEF_CREATED,
                                                        event);
                                        log.info("Published UserCreatedEvent for chef userId={}", user.getId());
                                }
                        });
                }
                log.info("Finished staff user creation for userId={}", user.getId());

                // 6. Return a success message
                return new MessageResponse("Staff user registered successfully.");
        }

        // ── Login ─────────────────────────────────────────────────────────────────

        public AuthResponse login(AuthRequest request) {
                // Delegates credential validation to DaoAuthenticationProvider.
                // The first argument is the email — Spring's internal parameter is named
                // "username" but here it holds the email value as the principal identifier.
                // Throws BadCredentialsException automatically on failure.
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new IllegalStateException(
                                                 "User authenticated but not found — this should never happen"));

                log.info("User logged in: id={}", user.getId());
                return new AuthResponse(jwtService.generateToken(user), user.getRole().name(), user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
        }
}
