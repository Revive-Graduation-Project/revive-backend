package com.restaurant.auth.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.restaurant.auth.domain.entity.User;

import java.util.Collection;
import java.util.List;

// This lives in your security config folder, far away from your database entities.
public class SecurityUser implements UserDetails {

    // The wrapper holds the dumb database file inside it
    private final User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    @Override
    public String getUsername() {
        // We translate Spring's request for a "username" into our "email"
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Translating our Role enum into the format Spring Security demands
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // The "Lying Code" is now safely quarantined inside this wrapper
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }
}