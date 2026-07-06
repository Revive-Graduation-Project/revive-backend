package com.restaurant.auth.config;

import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;
import com.restaurant.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                           @org.springframework.beans.factory.annotation.Value("${app.admin.default-email:admin@revive.com}") String adminEmail,
                                           @org.springframework.beans.factory.annotation.Value("${app.admin.default-password:admin123}") String adminPassword) {
        return args -> {
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .firstName("System")
                        .lastName("Admin")
                        .build();
                userRepository.save(admin);
                log.info("Default admin user created: {}", adminEmail);
            }
        };
    }
}
