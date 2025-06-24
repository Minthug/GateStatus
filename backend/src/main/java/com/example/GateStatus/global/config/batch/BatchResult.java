package com.example.GateStatus.global.config.batch;

import java.util.List;

public record BatchResult(
        int successCount,
        int errorCount,
        List<String> errorIds
) {
    public double getSuccessRate() {
        int total = successCount + errorCount;
        return total > 0 ? (double) successCount / total * 100 : 0;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }
}
