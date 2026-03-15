package com.restaurant.kitchen.repository;

import com.restaurant.kitchen.entity.KitchenTicket;
import com.restaurant.kitchen.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenTicketRepository extends JpaRepository<KitchenTicket, Long> {
   List<KitchenTicket> findByStatusNot(TicketStatus status); // search for active tickets
}
