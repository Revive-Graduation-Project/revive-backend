package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long> {

   Optional<KitchenTicket> findByOrderId(Long orderId);
   List<KitchenTicket> findByStatusIn(List<TicketStatus> statuses);
}
