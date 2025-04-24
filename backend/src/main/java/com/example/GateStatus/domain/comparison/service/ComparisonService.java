package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonService {

    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementRepository;
    private final IssueRepository issueRepository;
    private final ProposedBillRepository billRepository;
    private final VoteRepository
}
