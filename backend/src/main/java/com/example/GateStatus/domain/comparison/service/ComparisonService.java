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
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            statements = statementRepository.findByFigureIdAndIssueIdsContainingAndStatementDateBetween(figureId, issueId,startDate, endDate);
        } else {
            statements = statementRepository.findByFigureIdAndStatementDateBetween(figureId, startDate, endDate, PageRequest.of(0, 20));
        }

        List<StatementInfo> statementInfos = statements.stream()
                .map(s -> new StatementInfo(
                        s.getId(),
                        s.getTitle(),
                        summarizeText(s.getContent(), 100),
                        s.getStatementDate(),
                        s.getSource()
                ))
                .collect(Collectors.toList());

        Map<String, Integer> keywordCounts = analyzeKeywords(statements);

        String mainStance = analyzeMainStance(statements);

        return new StatementComparisonData(
                statementInfos,
                statements.size(),
                mainStance,
                keywordCounts
        );
    }

    /**
     * 투표 비교 데이터 조회
     */
    private VoteComparisonData getVoteComparison(Long figureId, String issueId, LocalDate startDate, LocalDate endDate) {
        List<Vote> votes;

        if (issueId != null && !issueId.isEmpty()) {
            IssueDocument issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다" + issueId));

            votes = voteRepository.findByFigureIdAndBillIdAndVoteDateBetween(
                    figureId, issue.getRelatedBillIds().stream().collect(Collectors.toList()),
                    startDate,
                    endDate
            );
        } else {
            votes = voteRepository.findByFigureIdAndVoteDateBetween(figureId, startDate, endDate);
        }

        int agreeCount = 0;
        int disagreeCount = 0;
        int abstainCount = 0;

        for (Vote vote : votes) {
            switch (vote.getVoteResult()) {
                case AGREE -> agreeCount++;
                case DISAGREE -> disagreeCount++;
                case ABSTAIN -> abstainCount++;
            }
        }

        double agreementRate = votes.isEmpty() ? 0 : (double) agreeCount / votes.size() * 100;

        List<VoteInfo> voteInfos = votes.stream()
                .map(v -> new VoteInfo(
                        v.getId().toString(),
                        v.getBill() != null ? v.getBill().getBillName() : "알 수 없음",
                        v.getVoteDate(),
                        v.getVoteResult().name()
                ))
                .collect(Collectors.toList());

        return new VoteComparisonData(
                voteInfos,
                agreeCount,
                disagreeCount,
                abstainCount,
                agreementRate
        );
    }

    /**
     * 법안 비교 데이터 조회
     */
    private BillComparisonData getBillComparison(Long figureId, String issueId, LocalDate startDate, LocalDate endDate) {
        List<ProposedBill> bills;

        if (issueId != null && !issueId.isEmpty()) {
            IssueDocument issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다" + issueId));

            bills = billRepository.findByProposerIdAndIdInAndProposeDateBetween(
                    figureId,
                    issue.getRelatedBillIds().stream().collect(Collectors.toList()),
                    startDate,
                    endDate
            );
        } else {
            bills = billRepository.findByProposerIdAndProposeDateBetween(figureId, startDate, endDate);
        }

        int passedCount = 0;
        for (ProposedBill bill : bills) {
            if (bill.getBillStatus() != null && bill.getBillStatus().isPassed()) {
                passedCount++;
            }
        }

        double passRate = bills.isEmpty() ? 0 : (double) passedCount / bills.size() * 100;

        List<BillInfo> billInfos = bills.stream()
                .map(b -> new BillInfo(
                        b.getId(),
                        b.getBillName(),
                        b.getProposeDate(),
                        b.getBillStatus() != null ? b.getBillStatus().name() : "알 수 없음",
                        b.getBillStatus() != null && b.getBillStatus().isPassed()
                ))
                .collect(Collectors.toList());

        return new BillComparisonData(
                billInfos,
                bills.size(),
                passedCount,
                passRate
        );
    }


}
