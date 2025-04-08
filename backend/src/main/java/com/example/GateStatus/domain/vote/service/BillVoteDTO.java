package com.example.GateStatus.domain.vote.service;

import com.fasterxml.jackson.databind.JsonNode;

public record BillVoteDTO(
        String billNo,         // 의안번호
        String billName,       // 의안명
        String proposer,       // 제안자
        String committee,      // 소관위원회
        String proposeDate,    // 제안일자
        String voteResult,     // 표결 결과
        String voteDate) {        // 표결일자

    public static BillVoteDTO fromJsonNode(JsonNode node) {
        return new BillVoteDTO(
                getTextValue(node, "BILL_NO"),
                getTextValue(node, "BILL_NM"),
                getTextValue(node, "PROPOSER"),
                getTextValue(node, "COMMITTEE_NM"),
                getTextValue(node, "PROPOSE_DT"),
                getTextValue(node, "RESULT_VOTE_MOD_NM"),
                getTextValue(node, "VOTE_DT")
        );
    }

    /**
     * JsonNode에서 특정 필드의 텍스트 값 추출
     * @param node
     * @param fieldName
     * @return
     */
    public static String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}
