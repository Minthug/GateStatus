package com.example.GateStatus.domain.proposedBill.service.request;

import com.example.GateStatus.domain.proposedBill.BillStatus;

import java.time.LocalDate;
import java.util.List;

public record AdvancedSearchRequest(
        String proposerName,
        String billName,
        String keyword,
        String partyName,
        BillStatus status,
        LocalDate proposeDateFrom,
        LocalDate proposeDateTo,
        List<String> committees,
        Boolean includeCoProposers
) {
}
