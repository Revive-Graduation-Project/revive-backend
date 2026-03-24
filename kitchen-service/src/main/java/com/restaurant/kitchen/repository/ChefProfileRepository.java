package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChefProfileRepository extends JpaRepository<ChefProfile, Long> {

    Optional<ChefProfile> findByAuthUserId(Long authUserId);
    List<ChefProfile> findByStatus(ChefStatus chefStatus);
}
