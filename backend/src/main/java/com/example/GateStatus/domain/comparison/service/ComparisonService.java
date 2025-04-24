package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.exception.NotFoundCompareException;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.IssueDocument;
import com.example.GateStatus.domain.issue.exception.NotFoundIssueException;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonService {

    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementRepository;
    private final IssueRepository issueRepository;
    private final ProposedBillRepository billRepository;
    private final VoteRepository voteRepository;

    /**
     * 비교 결과 생성
     * @param request
     * @return
     */
    @Transactional
    public ComparisonResult compareByIssue(ComparisonRequest request) {

        // 비교할 정치인 목록 조회
        List<Figure> figures = figureRepository.findAllById(request.figureIds());
        if (figures.isEmpty()) {
            throw new NotFoundCompareException("비교할 정치인이 존재하지 않습니다");
        }

        // 이슈 정보 조회(지정된 경우)
        IssueInfo issueInfo = null;
        if (request.issueId() != null && !request.issueId().isEmpty()) {
            IssueDocument issue = issueRepository.findById(request.issueId())
                    .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다 " + request.issueId()));
            issueInfo = new IssueInfo(
                    issue.getId(),
                    issue.getName(),
                    issue.getDescription(),
                    issue.getCategoryName()
            );
        }

        // 날짜 범위 설정
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now().minusYears(1);
        LocalDate endDate = request.endDate() != null ? request.endDate() : LocalDate.now();

        // 정치인별 비교 데이터 생성
        List<FigureComparisonData> figureDataList = new ArrayList<>();
        for (Figure figure : figures) {
            StatementComparisonData statements = null;
            VoteComparisonData votes = null;
            BillComparisonData bills = null;

            // 요청된 비교 유형에 따라 데이터 생성
            List<ComparisonType> types = request.comparisonTypes();
            if (types == null || types.contains("STATEMENT")) {
                statements = getStatementComparison(figure.getId(), request.issueId(), startDate, endDate);
            }

            if (types == null || types.contains("VOTE")) {
                votes = getVoteComparison(figure.getId(), request.issueId(), startDate, endDate);
            }

            if (types == null || types.contains("BILL")) {
                bills = getBillComparison(figure.getId(), request.issueId(), startDate, endDate);
            }

            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("activeDays", calculateActiveDays(figure.getId(), startDate, endDate));

            FigureComparisonData figureData = new FigureComparisonData(
                    figure.getId(),
                    figure.getName(),
                    figure.getFigureParty() != null ? figure.getFigureParty().getPartyName() : "소속 정당 없음",
                    statements,
                    votes,
                    bills,
                    additionalData
            );

            figureDataList.add(figureData);
        }

        Map<String, Object> summaryData = generateSummaryData(figureDataList, issueInfo);
        return new ComparisonResult(figureDataList, issueInfo, summaryData);
    }

    private StatementComparisonData getStatementComparison(Long figureId, String issueId, LocalDate startDate, LocalDate endDate) {
        List<StatementDocument> statements;

        if (issueId != null && !issueId.isEmpty()) {
            statements = statementRepository.
        }
    }
}
