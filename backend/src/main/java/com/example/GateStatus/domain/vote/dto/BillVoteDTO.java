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

    public static BillVoteDTO fromJsonNode(JsonNode node) {
        String voteResult = getTextValue(node, "RESULT_VOTE_MOD_MM");

        return new BillVoteDTO(
                getTextValue(node, "BILL_NO"),
                getTextValue(node, "BILL_NM"),
                getTextValue(node, "PROPOSER"),
                getTextValue(node, "COMMITTEE_NM"),
                getTextValue(node, "PROPOSE_DT"),
                voteResult,
                getTextValue(node, "VOTE_DT"),
                VoteResultType.fromString(voteResult),
                getTextValue(node, "PROC_RESULT"),
                getTextValue(node, "LINK_URL")
        );
    }
}
