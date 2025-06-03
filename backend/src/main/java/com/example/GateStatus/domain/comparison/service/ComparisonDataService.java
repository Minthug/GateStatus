package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.common.DateRange;
import com.example.GateStatus.domain.comparison.exception.NotFoundCompareException;
import com.example.GateStatus.domain.comparison.service.response.FigureComparisonData;
import com.example.GateStatus.domain.comparison.service.response.IssueInfo;
import com.example.GateStatus.domain.comparison.service.response.StatementComparisonData;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.IssueDocument;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.metrics.Stat;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
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
    private final FigureRepository figureRepository;


    public ComparisonService.ComparisonRawData fetchAllComparisonData(List<Long> figureIds,
                                                                      ComparisonService.ComparisonContext context,
                                                                      DateRange dateRange) {

        log.info("비교 데이터 배치 조회 시작: 정치인 {}명, 기간 {}", figureIds.size(), dateRange.getShortDescription());

        long startTime = System.currentTimeMillis();

        Map<Long, Figure> figureMap = getFiguresMap(figureIds);

        Map<Long, List<StatementDocument>> statementsMap = fetchStatementsBatch(figureIds, context, dateRange);
        Map<Long, List<Vote>> votesMap = fetchVotesBatch(figureIds, context, dateRange);
        Map<Long, List<ProposedBill>> billsMap = fetchBillsBatch(figureIds, context, dateRange);

        long endTime = System.currentTimeMillis();
        log.info("비교 데이터 조회 완료: {}ms, 발언 {}건, 투표 {}건, 법안 {}건",
                endTime - startTime,
                statementsMap.values().stream().mapToInt(List::size).sum(),
                votesMap.values().stream().mapToInt(List::size).sum(),
                billsMap.values().stream().mapToInt(List::size).sum());

        return ComparisonService.ComparisonRawData.builder()
                .figures(figureMap)
                .statements(statementsMap)
                .votes(votesMap)
                .bills(billsMap)
                .dateRange(dateRange)
                .context(context)
                .build();
    }

    private Map<Long, List<StatementDocument>> fetchStatementsBatch(List<Long> figureIds,
                                                                    ComparisonService.ComparisonContext context,
                                                                    DateRange dateRange) {
        List<StatementDocument> allStatements;

        if (context.hasTargetIssue()) {
            allStatements = statementRepository.findByFigureIdInAndIssueIdsContainingAndStatementDateBetween(
                    figureIds,
                    context.getIssueId(),
                    dateRange.getStartDate(),
                    dateRange.getEndDate()
            );
        } else {
            int pageSize = calculateOptimalPageSize(figureIds.size());
            allStatements = statementRepository.findByFigureIdInAndStatementDateBetween(
                    figureIds,
                    dateRange.getStartDate(),
                    dateRange.getEndDate(),
                    PageRequest.of(0, pageSize));
        }

        Map<Long, List<StatementDocument>> statementsMap = allStatements.stream()
                .collect(Collectors.groupingBy(StatementDocument::getFigureId));

        figureIds.forEach(figureId -> statementsMap.putIfAbsent(figureId, new ArrayList<>()));
        log.debug("발언 데이터 조회: 총 {}건, 정치인별 평균 {}건",
                allStatements.size(), allStatements.size() / (double) figureIds.size());

        return statementsMap;
    }


    private Map<Long, List<ProposedBill>> fetchBillsBatch(
            List<Long> figureIds, ComparisonService.ComparisonContext context, DateRange dateRange) {

        List<ProposedBill> allBills;

        if (context.hasTargetIssue()) {
            List<Long> relatedBillIds = getRelatedBillIdsAsLong(context.getIssueId());
            if (!relatedBillIds.isEmpty()) {
                allBills = billRepository.findByProposerIdInAndIdInAndProposeDateBetween(
                        figureIds, relatedBillIds, dateRange.getStartDate(), dateRange.getEndDate());
            } else {
                allBills = billRepository.findByProposerIdInAndProposeDateBetween(
                        figureIds, dateRange.getStartDate(), dateRange.getEndDate());
            }
        } else {
            allBills = billRepository.findByProposerIdInAndProposeDateBetween(
                    figureIds, dateRange.getStartDate(), dateRange.getEndDate());
        }
        Map<Long, List<ProposedBill>> billsMap = allBills.stream()
                .collect(Collectors.groupingBy(ProposedBill::getId));

        figureIds.forEach(figureId ->
                billsMap.putIfAbsent(figureId, new ArrayList<>()));

        log.debug("법안 데이터 조회: 총 {}건, 정치인별 평균 {}건",
                allBills.size(), allBills.size() / (double) figureIds.size());
        return billsMap;
    }


    private Map<Long, List<Vote>> fetchVotesBatch(
            List<Long> figureIds, ComparisonService.ComparisonContext context, DateRange dateRange) {

        List<Vote> allVotes;

        if (context.hasTargetIssue()) {
            // 특정 이슈 관련 투표 조회
            List<String> relatedBillIds = getRelatedBillIds(context.getIssueId());
            if (!relatedBillIds.isEmpty()) {
                allVotes = voteRepository.findByFigureIdInAndBillBillNoInAndVoteDateBetween(
                        figureIds, relatedBillIds, dateRange.getStartDate(), dateRange.getEndDate());
            } else {
                allVotes = voteRepository.findByFigureIdInAndVoteDateBetween(
                        figureIds, dateRange.getStartDate(), dateRange.getEndDate());
            }
        } else {
            // 전체 투표 조회
            allVotes = voteRepository.findByFigureIdInAndVoteDateBetween(
                    figureIds, dateRange.getStartDate(), dateRange.getEndDate());
        }

        Map<Long, List<Vote>> votesMap = allVotes.stream()
                .collect(Collectors.groupingBy(vote -> vote.getFigure().getId()));

        figureIds.forEach(figureId -> votesMap.putIfAbsent(figureId, new ArrayList<>()));


        log.debug("투표 데이터 조회: 총 {}건, 정치인별 평균 {}건",
                allVotes.size(), allVotes.size() / (double) figureIds.size());

        return votesMap;
    }


    private List<String> getRelatedBillIds(String issueId) {
        return issueRepository.findById(issueId)
                .map(IssueDocument::getRelatedBillIds)
                .orElse(Collections.emptyList());
    }

    private Map<Long, Figure> getFiguresMap(List<Long> figureIds) {
        List<Figure> figures = figureRepository.findAllById(figureIds);

        if (figures.size() != figureIds.size()) {
            Set<Long> foundIds = figures.stream()
                    .map(Figure::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = figureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());

            throw new NotFoundCompareException("존재하지 않는 정치인: " + missingIds);
        }
        return figures.stream()
                .collect(Collectors.toMap(Figure::getId, Function.identity()));
    }

    private List<Long> getRelatedBillIdsAsLong(String issueId) {
        return getRelatedBillIds(issueId).stream()
                .map(this::safelyParseLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Long safelyParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("법안 ID Long 변환 실패: {}", value);
            return null;
        }
    }

    private int calculateOptimalPageSize(int figureCount) {
        int baseSize = figureCount * 100;
        return Math.max(500, Math.min(5000, baseSize));
    }
}
