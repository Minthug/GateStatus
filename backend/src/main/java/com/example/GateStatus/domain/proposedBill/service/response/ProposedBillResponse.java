package com.example.GateStatus.domain.proposedBill.service.response;

import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBill;

import java.time.LocalDate;
import java.util.List;

public record ProposedBillResponse(Long id,
                                   Long proposerId,
                                   String proposerName,
                                   String billId,
                                   String billNo,
                                   String billName,
                                   LocalDate proposeDate,
                                   String summary,
                                   String content,
                                   String billUrl,
                                   BillStatus billStatus,
                                   LocalDate processDate,
                                   String processResult,
                                   List<String> coProposers,
                                   String committee,
                                   Integer viewCount) {

    public static ProposedBillResponse from(ProposedBill bill) {
        return new ProposedBillResponse(
                bill.getId(),
                bill.getProposer().getId(),
                bill.getProposer().getName(),
                bill.getBillId(),
                bill.getBillNo(),
                bill.getBillName(),
                bill.getProposeDate(),
                bill.getSummary(),
                bill.getContent(),
                bill.getBillUrl(),
                bill.getBillStatus(),
                bill.getProcessDate(),
                bill.getProcessResult(),
                bill.getCoProposers(),
                bill.getCommittee(),
                bill.getViewCount()
        );
    }
}
