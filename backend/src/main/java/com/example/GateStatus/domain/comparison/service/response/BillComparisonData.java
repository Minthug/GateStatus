package com.example.GateStatus.domain.comparison.service.response;

import java.util.List;

public record BillComparisonData(
        List<BillInfo> proposedBills,
        int proposedCount,
        int passedCount,
        double passRate
) {
}
