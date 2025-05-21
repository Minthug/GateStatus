package com.example.GateStatus.domain.vote.dto;

import java.time.LocalDate;

public record PoliticianVoteDTO(
        String politicianName,
        String party,
        String voteResult,
        LocalDate voteDate
) {
}
