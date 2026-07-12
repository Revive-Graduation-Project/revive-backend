package com.restaurant.inventory.service;

import com.restaurant.inventory.dto.ImportJobDto;
import com.restaurant.inventory.entity.ImportJob;
import com.restaurant.inventory.helper.CsvParserHelper;
import com.restaurant.inventory.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportJobService {

    private static final long HEARTBEAT_TIMEOUT_MINUTES = 5;

    private final ImportJobRepository importJobRepository;
    private final MenuCsvService menuCsvService;

    /**
     * Creates a PENDING ImportJob, persists it, then fires the async pipeline.
     * Returns the jobId and a human-readable message so the controller can
     * forward them straight to the client.
     */
    public Map<String, Object> startImport(List<CsvParserHelper.MealCsvEntry> meals) {
        ImportJob job = ImportJob.builder()
                .id(UUID.randomUUID().toString())
                .status(ImportJob.ImportStatus.PENDING)
                .totalRecords(meals.size())
                .processedRecords(0)
                .message("Import queued")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        importJobRepository.save(job);

        // Fire-and-forget — @Async in MenuCsvService handles threading
        menuCsvService.importFromJsonAsync(meals, job.getId());

        log.info("Import job created — jobId={}, meals={}", job.getId(), meals.size());
        return Map.of(
                "jobId", job.getId(),
                "message", "Import started — " + meals.size() + " meals queued for processing."
        );
    }

    /**
     * Returns the current status of a single job, or empty if not found.
     */
    public Optional<ImportJobDto> getJobStatus(String jobId) {
        return importJobRepository.findById(jobId)
                .map(ImportJobDto::from);
    }

    /**
     * Returns a page of jobs ordered by creation time descending.
     */
    public Page<ImportJobDto> getAllJobs(Pageable pageable) {
        return importJobRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ImportJobDto::from);
    }

    /**
     * Applies the heartbeat-timeout rule, then returns the first still-active job.
     *
     * Any PROCESSING/PENDING job whose updatedAt is older than HEARTBEAT_TIMEOUT_MINUTES
     * is automatically marked FAILED (server crash / restart scenario) before the
     * active job is resolved.
     */
    @Transactional
    public Optional<ImportJobDto> getActiveJob() {
        List<ImportJob> activeJobs = importJobRepository.findActiveJobs();

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
        for (ImportJob job : activeJobs) {
            if (job.getUpdatedAt() != null && job.getUpdatedAt().isBefore(cutoff)) {
                job.setStatus(ImportJob.ImportStatus.FAILED);
                job.setMessage("Job timed out — server may have restarted.");
                importJobRepository.save(job);
                log.warn("Job {} timed out (no heartbeat for >{}min) — marked FAILED",
                        job.getId(), HEARTBEAT_TIMEOUT_MINUTES);
            }
        }

        return activeJobs.stream()
                .filter(j -> j.getStatus() == ImportJob.ImportStatus.PROCESSING
                          || j.getStatus() == ImportJob.ImportStatus.PENDING)
                .findFirst()
                .map(ImportJobDto::from);
    }

    /**
     * Marks a job as CANCELED if it is still in a cancellable state.
     * The async pipeline in MenuCsvService polls the DB before each meal
     * and will abort when it sees the CANCELED status.
     */
    @Transactional
    public Optional<ImportJobDto> cancelJob(String jobId) {
        int updated = importJobRepository.updateStatusIf(
                jobId,
                ImportJob.ImportStatus.CANCELED,
                "Cancelled by admin.",
                List.of(ImportJob.ImportStatus.PROCESSING, ImportJob.ImportStatus.PENDING)
        );
        if (updated > 0) {
            log.info("Job {} cancelled by admin", jobId);
        }
        return importJobRepository.findById(jobId).map(ImportJobDto::from);
    }
}
