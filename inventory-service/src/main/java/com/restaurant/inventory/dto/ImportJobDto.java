package com.restaurant.inventory.dto;

import com.restaurant.inventory.entity.ImportJob.ImportStatus;
import java.time.LocalDateTime;

public record ImportJobDto(
    String id,
    ImportStatus status,
    int totalRecords,
    int processedRecords,
    String message,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ImportJobDto from(com.restaurant.inventory.entity.ImportJob job) {
        return new ImportJobDto(
            job.getId(),
            job.getStatus(),
            job.getTotalRecords(),
            job.getProcessedRecords(),
            job.getMessage(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }
}
