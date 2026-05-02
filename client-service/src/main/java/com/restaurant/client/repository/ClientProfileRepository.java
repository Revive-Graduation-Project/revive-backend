package com.restaurant.client.repository;

import com.restaurant.client.domain.entity.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
}
