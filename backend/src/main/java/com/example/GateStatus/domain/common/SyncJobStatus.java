package com.example.GateStatus.domain.common;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class SyncJobStatus {
    private final String jobId;
    private int totalTasks;
    private int completedTasks = 0;
    private int successCount = 0;
    private int failCount = 0;
    private int syncCount = 0;
    private boolean completed = false;
    private boolean error = false;
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

    public synchronized void addSyncCount(int count) {
        this.syncCount += count;
    }

    /**
     * 모든 작업이 완료되었는지 확인
     * @return 완료 여부
     */
    public boolean isAllTasksCompleted() {
        return completedTasks >= totalTasks;
    }

}
