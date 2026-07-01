package com.restaurant.order.repository;

import com.restaurant.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = "items")
    List<Order> findDistinctByClientIdOrderByCreatedAtDesc(Long clientId);
}
