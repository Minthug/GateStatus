package com.example.GateStatus.domain.proposedBill.service.response;

import com.example.GateStatus.domain.proposedBill.BillStatus;

import java.util.List;
import java.util.Map;

public record BillStatisticsResponse(
        long totalBills,
        Map<BillStatus, Long> statusDistribution,
        long billsThisMonth,
        long billsThisYear,
        String mostActiveFigure,
        Map<String, Long> partyDistribution,
        List<String> topKeywords
) {
}
