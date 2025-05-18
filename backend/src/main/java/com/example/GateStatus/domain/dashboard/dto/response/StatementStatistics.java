package com.example.GateStatus.domain.dashboard.dto.response;

import java.util.Map;

public record StatementStatistics(
        int total,
        Map<String, Integer> categoryDistribution,
        String mostFrequentCategory
) {
}
