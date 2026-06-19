package com.restaurant.auth.service;

import com.restaurant.auth.domain.enums.Role;
import com.restaurant.auth.dto.UserResponse;
import com.restaurant.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns all users in the system.
     * Intended for ADMIN use only.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Returns only staff members (CHEF and MANAGER roles).
     * Intended for MANAGER use.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getStaffUsers() {
        log.info("Fetching staff users");
        return userRepository.findByRoleIn(List.of(Role.CHEF, Role.MANAGER))
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}
