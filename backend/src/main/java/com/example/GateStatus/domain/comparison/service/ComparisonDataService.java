package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.comparison.service.response.FigureComparisonData;
import com.example.GateStatus.domain.comparison.service.response.IssueInfo;
import com.example.GateStatus.domain.comparison.service.response.StatementComparisonData;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonDataService {

    private final StatementMongoRepository statementRepository;
    private final VoteRepository voteRepository;
    private final ProposedBillRepository billRepository;
    private final IssueRepository issueRepository;
    private final PoliticalAnalysisService analysisService;


    public List<FigureComparisonData> createFigureComparisonData(List<Figure> figures, IssueInfo issueInfo, ComparisonService.DateRange dateRange) {
        if (figures == null || figures.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> figureIds = figures.stream()
                .map(Figure::getId)
                .collect(Collectors.toList());


        log.info("정치인별 비교 데이터 생성 시작: 대상 {}명, 기간 {}~{}",
                figureIds.size(), dateRange.startDate(), dateRange.endDate());


    }
}
