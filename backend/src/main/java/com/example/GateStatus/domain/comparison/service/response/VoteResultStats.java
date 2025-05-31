package com.example.GateStatus.domain.comparison.service.response;

public record VoteResultStats(
        int agree,
        int disagree,
        int abstain,
        int absent,
        double agreeRate,
        double participationRate
) {
}
