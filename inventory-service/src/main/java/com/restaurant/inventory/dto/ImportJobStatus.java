package com.restaurant.inventory.dto;

/**
 * Snapshot of a background import job's current state.
 */
public record ImportJobStatus(
        String jobId,
        JobState state,
        String errorMessage
) {
    public enum JobState {
        PENDING, PROCESSING, DONE, FAILED, CANCELLED
    }

    public boolean isTerminal() {
        return state == JobState.DONE || state == JobState.FAILED || state == JobState.CANCELLED;
    }
}
