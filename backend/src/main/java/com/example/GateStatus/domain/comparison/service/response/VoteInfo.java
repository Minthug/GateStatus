package com.example.GateStatus.domain.comparison.service.response;

import java.time.LocalDate;

public record VoteInfo(
        String voteId,
        String billName,
        LocalDate voteDate,
        String voteResult
) {
}
