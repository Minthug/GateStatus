package com.example.GateStatus.domain.common;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class SyncJobStatus {
    private final String jobId;
    private int totalTasks;
    private int completedTasks;
    private int successCount;
    private int failCount;
    private boolean completed;
    private boolean error;
    private String errorMessage;
    private final LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;


    public SyncJobStatus(String jobId) {
        this.jobId = jobId;
    }

    public void incrementCompletedTasks() {
        this.completedTasks++;
    }

    public void incrementSuccessCount() {
        this.successCount++;
    }

    public void incrementFailCount() {
        this.failCount++;
    }

    public int getProgressPercentage() {
        if (totalTasks == 0) return 100;
        return (int) ((completedTasks * 100.0) / totalTasks);
    }

    public Duration getElapsedTime() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end);
    }
}
