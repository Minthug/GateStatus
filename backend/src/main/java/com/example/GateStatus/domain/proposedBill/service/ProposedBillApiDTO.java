package com.example.GateStatus.domain.proposedBill.service;

import java.util.List;

public record ProposedBillApiDTO(String billId,
                                 String billNo,
                                 String billName,
                                 String proposerName,
                                 String proposedDate,
                                 String summary,
                                 String billUrl,
                                 String billStatus,
                                 String processDate,
                                 String processResult,
                                 String committee,
                                 List<String> coProposers) {
}
