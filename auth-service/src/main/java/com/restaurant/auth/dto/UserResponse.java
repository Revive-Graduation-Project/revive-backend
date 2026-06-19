package com.restaurant.auth.dto;

import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.domain.enums.Role;

public record UserResponse(
        Long id,
        String email,
        Role role,
        Boolean isActive
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive()
        );
    }
}
