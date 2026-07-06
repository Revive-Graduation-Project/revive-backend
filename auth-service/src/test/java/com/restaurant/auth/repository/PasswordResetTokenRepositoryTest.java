package com.restaurant.auth.repository;

import com.restaurant.auth.domain.entity.PasswordResetToken;
import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("testreset@example.com")
                .password("hashedPass")
                .firstName("Test")
                .lastName("User")
                .role(Role.CLIENT)
                .build();
        savedUser = userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testFindByToken_ReturnsToken_WhenTokenExists() {
        // Arrange
        String tokenString = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenString)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        tokenRepository.save(token);

        // Act
        Optional<PasswordResetToken> foundToken = tokenRepository.findByToken(tokenString);

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo(tokenString);
        assertThat(foundToken.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void testFindByToken_ReturnsEmpty_WhenTokenDoesNotExist() {
        // Act
        Optional<PasswordResetToken> foundToken = tokenRepository.findByToken("non-existent-token");

        // Assert
        assertThat(foundToken).isEmpty();
    }

    @Test
    void testDeleteByUser_DeletesToken() {
        // Arrange
        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        tokenRepository.save(token);

        // Act
        tokenRepository.deleteByUser(savedUser);
        Optional<PasswordResetToken> foundToken = tokenRepository.findByToken(token.getToken());

        // Assert
        assertThat(foundToken).isEmpty();
    }
}
