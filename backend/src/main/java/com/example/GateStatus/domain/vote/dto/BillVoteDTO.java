package com.example.GateStatus.domain.vote.dto;

import com.example.GateStatus.domain.vote.VoteResultType;
import com.fasterxml.jackson.databind.JsonNode;

import static com.example.GateStatus.domain.common.JsonUtils.getTextValue;

public record BillVoteDTO(
        String billNo,         // 의안번호
        String billName,       // 의안명
        String proposer,       // 제안자
        String committee,      // 소관위원회
        String proposeDate,    // 제안일자
        String voteResult,     // 표결 결과
        String voteDate,
        VoteResultType voteResultType,
        String billStatus,
        String billUrl) {        // 표결일자

}
