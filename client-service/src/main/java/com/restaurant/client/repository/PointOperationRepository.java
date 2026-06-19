package com.restaurant.client.repository;

import com.restaurant.client.domain.entity.PointOperation;
import com.restaurant.client.domain.enums.PointOperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointOperationRepository extends JpaRepository<PointOperation, Long> {
    boolean existsByOrderIdAndOperationType(Long orderId, PointOperationType operationType);
}
