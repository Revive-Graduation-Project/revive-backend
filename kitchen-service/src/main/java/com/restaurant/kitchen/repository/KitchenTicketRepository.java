package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.KitchenTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long> {
   // List<Ticket> findByStatusNot(TicketStatus status);
}
