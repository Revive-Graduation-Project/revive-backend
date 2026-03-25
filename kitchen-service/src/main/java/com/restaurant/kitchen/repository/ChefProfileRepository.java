package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.ChefProfile;
import com.restaurant.kitchen.enums.ChefStatus;
import com.restaurant.kitchen.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ChefProfileRepository extends JpaRepository<ChefProfile, Long> {

    Optional<ChefProfile> findByAuthUserId(Long authUserId);

    @Query("""
        SELECT c FROM ChefProfile c
        LEFT JOIN c.assignedTickets t ON t.status != :ticketStatus
        WHERE c.status = :chefStatus
        GROUP BY c
        ORDER BY COUNT(t) ASC
    """)
    List<ChefProfile> findMostAvailableActiveChefs(
            @Param("chefStatus") ChefStatus chefStatus,
            @Param("ticketStatus") TicketStatus ticketStatus,
            Pageable pageable
    );
}
