package com.restaurant.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.inventory.entity.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    Page<ImportJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT j FROM ImportJob j WHERE j.status IN ('PROCESSING', 'PENDING') ORDER BY j.createdAt DESC")
    List<ImportJob> findActiveJobs();

    @Modifying
    @Query("UPDATE ImportJob j SET j.status = :newStatus, j.message = :message, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id AND j.status IN :expectedStatuses")
    int updateStatusIf(@Param("id") String id, @Param("newStatus") ImportJob.ImportStatus newStatus, @Param("message") String message, @Param("expectedStatuses") List<ImportJob.ImportStatus> expectedStatuses);

    @Modifying
    @Query("UPDATE ImportJob j SET j.status = :newStatus, j.message = :message, j.processedRecords = :processed, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id AND j.status IN :expectedStatuses")
    int updateStatusAndProgressIf(@Param("id") String id, @Param("newStatus") ImportJob.ImportStatus newStatus, @Param("message") String message, @Param("processed") int processed, @Param("expectedStatuses") List<ImportJob.ImportStatus> expectedStatuses);
    @Modifying
    @Query("UPDATE ImportJob j SET j.status = :newStatus, j.message = :message, j.totalRecords = :total, j.processedRecords = 0, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :id AND j.status IN :expectedStatuses")
    int startProcessingIf(@Param("id") String id, @Param("newStatus") ImportJob.ImportStatus newStatus, @Param("message") String message, @Param("total") int total, @Param("expectedStatuses") List<ImportJob.ImportStatus> expectedStatuses);
}
