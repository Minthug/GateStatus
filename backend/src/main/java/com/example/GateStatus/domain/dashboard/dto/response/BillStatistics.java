package com.example.GateStatus.domain.dashboard.dto.response;

public record BillStatistics(
        int total,
        int passed,
        int rejected,
        int withdrawn,
        int alternative,
        int processing,
        double passRate
) {
}
