package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.exception.NotFoundCompareException;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.IssueCategory;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

        // 카테고리 정보 처리
        IssueCategory category = null;
        List<IssueDocument> categoryIssues = null;
        if (request.category() != null && !request.category().isEmpty()) {
            try {
                category = IssueCategory.fromCode(request.category());
                categoryIssues = issueRepository.findByCategoryCodeAndIsActiveTrue(request.category());
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 카테고리 코드: {}", request.category());
            }
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

        // 요약 데이터 생성 (카테고리 정보 포함)
        Map<String, Object> summaryData = generateSummaryData(figureDataList, issueInfo, category, categoryIssues, request);
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

            votes = voteRepository.findByFigureIdAndBillBillNoInAndVoteDateBetween(
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

    private Object calculateActiveDays(Long figureId, LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> statementDates = statementRepository.findByFigureIdAndStatementDateBetween(figureId, startDate, endDate)
                .stream().map(StatementDocument::getStatementDate)
                .collect(Collectors.toSet());

        Set<LocalDate> voteDates = voteRepository.findByFigureIdAndVoteDateBetween(figureId, startDate, endDate)
                .stream().map(Vote::getVoteDate).collect(Collectors.toSet());

        Set<LocalDate> billDates = billRepository.findByProposerIdAndProposeDateBetween(figureId, startDate, endDate)
                .stream().map(ProposedBill::getProposeDate).collect(Collectors.toSet());

        Set<LocalDate> allDates = new HashSet<>(statementDates);
        allDates.addAll(voteDates);
        allDates.addAll(billDates);

        return allDates.size();
    }

    /**
     * 비교 데이터의 요약 정보 생성
     * @param figureDataList 정치인별 비교 데이터 목록
     * @param issueInfo 이슈 정보
     * @param category 카테고리 정보
     * @param categoryIssues 카테고리에 속한 이슈 목록
     * @param request 원본 비교 요청 정보
     * @return 요약 정보
     */
    private Map<String, Object> generateSummaryData(List<FigureComparisonData> figureDataList, IssueInfo issueInfo, IssueCategory category,
                                                    List<IssueDocument> categoryIssues, ComparisonRequest request) {
        Map<String, Object> summary = new HashMap<>();

        if (category != null && categoryIssues != null) {
            Map<String, Object> categoryInfo = new HashMap<>();
            categoryInfo.put("code", category.getCode());
            categoryInfo.put("name", category.getDisplayName());
            categoryInfo.put("issueCount", categoryIssues.size());

            List<Map<String, String>> topIssues = categoryIssues.stream()
                    .limit(5)
                    .map(issue -> Map.of(
                            "id", issue.getId(),
                            "name", issue.getName()
                    ))
                    .collect(Collectors.toList());

            categoryInfo.put("topIssues", topIssues);

            summary.put("category", categoryInfo);
        }

        if (!figureDataList.isEmpty()) {
            FigureComparisonData mostActive = figureDataList.stream()
                    .max(Comparator.comparingInt(f -> (Integer) f.additionalData().get("activeDays")))
                    .orElse(null);

            if (mostActive != null) {
                summary.put("mostActiveFigureId", mostActive.figureId());
                summary.put("mostActiveFigureName", mostActive.figureName());
            }

            LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now().minusYears(1);
            LocalDate endDate = request.endDate() != null ? request.endDate() : LocalDate.now();
            summary.put("analysisStartDate", startDate.toString());
            summary.put("analysisEndDate", endDate.toString());
            summary.put("analysisPeriodDays", ChronoUnit.DAYS.between(startDate, endDate));
        }

        if (figureDataList.stream().anyMatch(f -> f.statements() != null)) {
            FigureComparisonData mostStatements = figureDataList.stream()
                    .filter(f -> f.statements() != null)
                    .max(Comparator.comparingInt(f -> f.statements().statementCount()))
                    .orElse(null);

            if (mostStatements != null) {
                summary.put("mostStatementsFigureId", mostStatements.figureId());
                summary.put("mostStatementsFigureName", mostStatements.figureName());
            }
        }

        if (figureDataList.stream().anyMatch(f -> f.bills() != null)) {
            FigureComparisonData mostBills = figureDataList.stream()
                    .filter(f -> f.bills() != null)
                    .max(Comparator.comparingInt(f -> f.bills().proposedCount()))
                    .orElse(null);

            if (mostBills != null) {
                summary.put("mostBillsFigureId", mostBills.figureId());
                summary.put("mostBillsFigureName", mostBills.figureName());
            }
        }

        return summary;
    }

    private String summarizeText(String text, int maxLength) {

        if (text == null) {
            return "";
        }

        if (text == null || text.length() <= maxLength) {
            return text;
        }

        int lastSentenceEnd = text.lastIndexOf(". ", maxLength - 3);
        if (lastSentenceEnd > maxLength / 2) {
            return text.substring(0, lastSentenceEnd + 1) + "...";
        }

        int lastSpace = text.lastIndexOf(" ", maxLength - 3);
        if (lastSpace > 0) {
            return text.substring(0, lastSpace) + "...";
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    @Cacheable(value = "keywordAnalysis", key = "#statements.hashCode()")
    public Map<String, Integer> analyzeKeywords(List<StatementDocument> statements) {

        // 의미 없는 불용어(stopwords) 필터링
        Set<String> stopwords = Set.of("이", "그", "저", "이것", "그것", "저것", "이런", "그런", "저런",
                "및", "등", "을", "를", "이다", "있다", "하다");

        return statements.stream()
                .filter(statement -> statement.getContent() != null && !statement.getContent().isEmpty())
                .flatMap(statement -> Arrays.stream(statement.getContent().split("\\s+")))
                .filter(word -> word.length() >= 2) // 너무 짧은 단어 제외
                .filter(word -> !stopwords.contains(word)) // 불용어 제외
                .filter(word -> !word.matches("\\d+")) // 숫자만 있는 단어 제외
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.summingInt(word -> 1)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private String analyzeMainStance(List<StatementDocument> statements) {
        if (statements.isEmpty()) {
            return "입장 정보 없음";
        }

        List<StatementDocument> recentStatements = statements.stream()
                .filter(s -> s.getContent() != null && !s.getContent().isEmpty())
                .sorted(Comparator.comparing(StatementDocument::getStatementDate).reversed())
                .limit(3)
                .collect(Collectors.toList());

        if (recentStatements.isEmpty()) {
            return "입장 정보 없음";
        }

        StringBuilder combinedContent = new StringBuilder();
        for (int i = 0; i < recentStatements.size(); i++) {
            String content = recentStatements.get(i).getContent();
            String importantSentence = extractImportantSentence(content);
            combinedContent.append(importantSentence);
            if (i < recentStatements.size() - 1) {
                combinedContent.append(" ");
            }
        }

        return summarizeText(combinedContent.toString(), 150);
    }

    private String extractImportantSentence(String text) {
        // 주요 문장 추출 로직
        String[] sentences = text.split("\\. ");
        for (String sentence : sentences) {
            if (sentence.contains("주장") || sentence.contains("입장") ||
                sentence.contains("생각") || sentence.contains("의견") ||
                sentence.contains("해결") || sentence.contains("방안")) {
                return sentence + ".";
            }
        }

        if (sentences.length > 0) {
            return sentences[0] + (sentences[0].endsWith(".") ? "" : ".");
        }

        return text;
    }

}
