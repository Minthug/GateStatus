package com.example.GateStatus.domain.vote.dto;

import java.util.List;

public record PartyVoteStatisticsDTO(
        String partyName,
        int agreeCount,
        int disagreeCount,
        int abstainCount,
        int absentCount,
        List<IssueCountDTO> topIssues
) {
}
