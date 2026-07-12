package com.restaurant.inventory.repository;

import com.restaurant.inventory.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    List<ImportJob> findAllByOrderByCreatedAtDesc();

    @Query("SELECT j FROM ImportJob j WHERE j.status IN ('PROCESSING', 'PENDING') ORDER BY j.createdAt DESC")
    List<ImportJob> findActiveJobs();
}
