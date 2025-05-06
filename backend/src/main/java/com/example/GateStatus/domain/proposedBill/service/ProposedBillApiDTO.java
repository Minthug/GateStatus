package com.example.GateStatus.domain.proposedBill.service;

import java.util.List;

public record ProposedBillApiDTO(
        String billId,           // 법안 고유 ID
        String billNo,           // 법안 번호
        String billName,         // 법안 이름
        String proposer,         // 대표 발의자 이름
        String proposedDate,     // 발의 일자 (문자열 형식, yyyy-MM-dd)
        String summary,          // 법안 요약
        String linkUrl,          // 법안 상세 URL
        String processResultCode, // 처리 결과 코드
        String processDate,      // 처리 일자 (문자열 형식, yyyy-MM-dd)
        String processResult,    // 처리 결과 내용
        String committeeName,    // 소관 위원회 이름
        List<String> coProposers) { // 공동 발의자 목록

    public ProposedBillApiDTO {
        // null인 경우 빈 리스트로 대체
        coProposers = coProposers != null ? coProposers : List.of();
    }
}
