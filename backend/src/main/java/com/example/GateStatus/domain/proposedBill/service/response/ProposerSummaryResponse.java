package com.example.GateStatus.domain.proposedBill.service.response;

import java.time.LocalDate;

public record ProposerSummaryResponse(
        Long proposerId,
        String proposerName,
        String partyName,
        int totalBills,
        int passedBills,
        double passRate,
        LocalDate lastBillDate
) {
}
