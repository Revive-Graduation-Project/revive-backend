package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.ChefProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChefProfileRepository extends JpaRepository<ChefProfile, Long> {
    Optional<ChefProfile> findByAuthUserId(Long authUserId);
}
