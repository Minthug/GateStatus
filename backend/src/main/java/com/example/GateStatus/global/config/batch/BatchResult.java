package com.example.GateStatus.global.config.batch;

import java.util.List;

public record BatchResult(
        int successCount,
        int errorCount,
        List<String> errorIds
) {
}
