package com.example.GateStatus.domain.dashboard.dto.response;

public record VoteStatistics(
        int agree,
        int disagree,
        int abstain,
        int absent,
        double agreeRate,
        double participationRate
) {
}
