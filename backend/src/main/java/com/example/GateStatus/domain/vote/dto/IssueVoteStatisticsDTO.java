package com.example.GateStatus.domain.vote.dto;

import java.util.Map;

public record IssueVoteStatisticsDTO(
        String issueCategory,
        int totalBills,
        int passedBills,
        int rejectedBills,
        Map<String, Integer> partySupport
) {
}
