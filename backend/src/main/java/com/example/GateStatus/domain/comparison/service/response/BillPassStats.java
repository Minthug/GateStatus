package com.example.GateStatus.domain.comparison.service.response;

public record BillPassStats(
        int total,
        int passed,
        double passRate
) {
}
