package com.restaurant.auth.mapper;

import com.restaurant.auth.domain.entity.User;
import com.restaurant.auth.dto.SignupRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper that bridges the domain entity and DTOs,
 * enforcing the controller → service → repository boundary.
 *
 * <p>The {@code toUser} method is provided for any future read endpoint
 * that must return user data without exposing the entity directly.</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link SignupRequest} to a {@link User} entity.
     * {@code email} is auto-mapped by name. Password hashing and role
     * assignment are handled by the service layer, so {@code password}
     * and {@code isActive} are excluded.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    User toUser(SignupRequest request);
}

