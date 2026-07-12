package com.restaurant.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportJob {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private ImportStatus status;

    private int totalRecords;
    private int processedRecords;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ImportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELED
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
