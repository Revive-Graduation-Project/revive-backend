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
    public CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("admin@revive.com").isEmpty()) {
                User admin = User.builder()
                        .email("admin@revive.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .firstName("System")
                        .lastName("Admin")
                        .build();
                userRepository.save(admin);
                log.info("Default admin user created: admin@revive.com / admin123");
            }
        };
    }
}
