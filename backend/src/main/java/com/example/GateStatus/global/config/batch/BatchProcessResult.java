package com.example.GateStatus.global.config.batch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record BatchProcessResult(
        int totalCount,
        int successCount,
        int errorCount,
        List<String> errorIds,
        LocalDateTime startTime,
        LocalDateTime endTime
) {

    public double getSuccessRate() {
        return totalCount == 0 ? 0.0 : (double) successCount / totalCount * 100;
    }

    public Duration getProcessingTime() {
        return Duration.between(startTime, endTime);
    }
}
