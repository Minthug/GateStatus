package com.example.GateStatus.domain.vote.dto;

public record BillDetailDTO(String billId,
                            String billNo,
                            String billName,
                            String committee,
                            String proposer,
                            String proposeDate,
                            String procResult,
                            String procDate,
                            String billUrl,
                            String detailContent,
                            String proposerInfo,
                            VoteResultDetail voteResult) {

    public static BillDetailDTO empty(String billNo) {
        return new BillDetailDTO(
                "",
                billNo,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new VoteResultDetail(0, 0, 0, 0, 0)
        );
    }

    public boolean isPassed() {
        return procResult != null &&
                (procResult.contains("원안가결") ||
                procResult.contains("수정가결") ||
                procResult.contains("대안반영폐기"));
    }
}
