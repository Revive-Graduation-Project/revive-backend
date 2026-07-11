package com.restaurant.inventory.service;

import com.restaurant.inventory.dto.ImportJobStatus;
import com.restaurant.inventory.dto.ImportJobStatus.JobState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for background CSV import job statuses.
 *
 * <p>Lifecycle: PENDING → PROCESSING → DONE | FAILED
 *
 * <p>Thread-safe via ConcurrentHashMap. Suitable for single-pod deployments
 * (Railway). If multi-pod scaling is ever needed, replace with Redis.
 */
@Component
@Slf4j
public class ImportJobStore {

    private final ConcurrentHashMap<String, ImportJobStatus> jobs = new ConcurrentHashMap<>();

    /**
     * Creates a new job in PENDING state and returns its ID.
     */
    public String createJob() {
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new ImportJobStatus(jobId, JobState.PENDING, null));
        log.debug("ImportJobStore: created job {}", jobId);
        return jobId;
    }

    /**
     * Transitions a job to PROCESSING.
     */
    public void markProcessing(String jobId) {
        update(jobId, new ImportJobStatus(jobId, JobState.PROCESSING, null));
    }

    /**
     * Transitions a job to DONE.
     */
    public void markDone(String jobId) {
        update(jobId, new ImportJobStatus(jobId, JobState.DONE, null));
        log.info("ImportJobStore: job {} completed successfully", jobId);
    }

    /**
     * Transitions a job to FAILED and records the error message.
     */
    public void markFailed(String jobId, String errorMessage) {
        update(jobId, new ImportJobStatus(jobId, JobState.FAILED, errorMessage));
        log.warn("ImportJobStore: job {} failed — {}", jobId, errorMessage);
    }

    /**
     * Returns the current status of a job, or empty if the jobId is unknown.
     */
    public Optional<ImportJobStatus> getStatus(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void update(String jobId, ImportJobStatus status) {
        if (!jobs.containsKey(jobId)) {
            log.warn("ImportJobStore: attempted to update unknown job {}", jobId);
            return;
        }
        jobs.put(jobId, status);
    }

    /**
     * Attempts to cancel an ongoing job.
     * @return true if successfully cancelled, false if the job was already finished or didn't exist.
     */
    public boolean cancelJob(String jobId) {
        ImportJobStatus current = jobs.get(jobId);
        if (current == null) {
            return false;
        }
        if (current.isTerminal()) {
            return false;
        }
        jobs.put(jobId, new ImportJobStatus(jobId, JobState.CANCELLED, "Import cancelled by user"));
        log.info("ImportJobStore: job {} cancelled", jobId);
        return true;
    }
}
