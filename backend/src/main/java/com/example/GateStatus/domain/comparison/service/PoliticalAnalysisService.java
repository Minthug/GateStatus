package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.comparison.service.response.BillPassStats;
import com.example.GateStatus.domain.comparison.service.response.VoteResultStats;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.VoteResultType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoliticalAnalysisService {

    private static final Set<String> STOPWORDS = Set.of(
            "이", "그", "저", "이것", "그것", "저것", "이런", "그런", "저런",
            "및", "등", "을", "를", "이다", "있다", "하다", "그리고", "또한", "그러나"
    );

    private static final int DEFAULT_KEYWORD_LIMIT = 10;
    private static final int MIN_WORD_LENGTH = 2;

    /**
     * 발언 내용에서 주요 키워드를 추출하고 빈도수를 계산
     *
     * @param contents 분석할 텍스트 내용 리스트
     * @param limit 반환할 키워드 개수 제한
     * @return 키워드별 빈도수 맵 (빈도순 정렬)
     */
    @Cacheable(value = "keywordAnalysis", key = "'contents_' + #contents.size() + '_' + #contents.hashCode() + '_' + #limit")
    public Map<String, Integer> analyzeKeywordsFromText(List<String> contents, int limit) {
        if (contents == null || contents.isEmpty()) {
            return new LinkedHashMap<>();
        }

        log.debug("텍스트 키워드 분석 시작: 문서 수={}, 제한={}", contents.size(), limit);

        return contents.stream()
                .filter(Objects::nonNull)
                .filter(content -> !content.trim().isEmpty())
                .flatMap(content -> Arrays.stream(content.split("\\s+")))
                .map(String::trim)
                .filter(word -> word.length() >= MIN_WORD_LENGTH)
                .filter(word -> !STOPWORDS.contains(word))
                .filter(word -> !word.matches("\\d+"))
                .filter(word -> !word.matches("[^가-힣a-zA-Z]+"))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().intValue(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 발언 문서 리스트에서 키워드 분석 (오버로드)
     * @param statements 발언 문서 리스트
     * @param limit 키워드 개수 제한
     * @return 키워드별 빈도수 맵
     */
    @Cacheable(value = "keywordAnalysis", key = "'statements_' + #statements.size() + '_' + #statements.hashCode() + '_' + #limit")
    public Map<String, Integer> analyzeKeywordsFromStatements(List<StatementDocument> statements, int limit) {
        if (statements == null || statements.isEmpty()) {
            return new LinkedHashMap<>();
        }

        log.debug("발언 문서 키워드 분석 시작: 문서 수={}, 제한={}", statements.size(), limit);

        List<String> contents = statements.stream()
                .map(StatementDocument::getContent)
                .filter(Objects::nonNull)
                .filter(content -> !content.trim().isEmpty())
                .collect(Collectors.toList());

        return analyzeKeywordsFromText(contents, limit);
    }


    /**
     * 발언 문서에서 기본 제한으로 키워드 분석
     * @param statements 발언 문서 리스트
     * @return 키워드별 빈도수 맵 (기본 10개 제한)
     */
    public Map<String, Integer> analyzeKeywordsFromStatements(List<StatementDocument> statements) {
        return analyzeKeywordsFromStatements(statements, DEFAULT_KEYWORD_LIMIT);
    }

    public Map<String, Integer> analyzeKeywordsFromSingleText(String text, int limit) {
        if (text == null || text.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }

        return analyzeKeywordsFromText(List.of(text), limit);
    }

    @Cacheable(value = "keywordAnalysis", key = "'figureStatements_' + #statementsByFigure.keySet().toString() + '_' + #limit")
    public Map<Long, Map<String, Integer>> analyzeKeywordsByFigure(Map<Long, List<StatementDocument>> statementsByFigure, int limit) {

        if (statementsByFigure == null || statementsByFigure.isEmpty()) {
            return new HashMap<>();
        }

        log.debug("정치인별 키워드 분석 시작: 정치인 수={}, 제한={}", statementsByFigure.size(), limit);

        return statementsByFigure.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> analyzeKeywordsFromStatements(entry.getValue(), limit)
                ));
    }

    /**
     * 텍스트를 지정된 길이로 요약
     * @param text 요약할 텍스트
     * @param maxLength 최대 길이
     * @return 요약된 텍스트
     */
    public String summarizeText(String text, int maxLength) {
        if (text  == null || text.trim().isEmpty()) {
            return "";
        }

        text = text.trim();
        if (text.length() <= maxLength) {
            return text;
        }

        int lastSentenceEnd = findLastSentenceEnd(text, maxLength);
        if (lastSentenceEnd > maxLength / 2) {
            return text.substring(0, lastSentenceEnd + 1) + "...";
        }

        int lastSpace = text.lastIndexOf(" ", maxLength - 3);
        if (lastSpace > 0) {
            return text.substring(0, lastSpace) + "...";
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    public VoteResultStats calculateVoteStats(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) {
            return new VoteResultStats(0, 0, 0, 0, 0.0, 0.0);
        }

        Map<VoteResultType, Long> resultCounts = votes.stream()
                .filter(vote -> vote.getVoteResult() != null)
                .collect(Collectors.groupingBy(
                        Vote::getVoteResult,
                        Collectors.counting()
                ));

        int agree = resultCounts.getOrDefault(VoteResultType.AGREE, 0L).intValue();
        int disagree = resultCounts.getOrDefault(VoteResultType.DISAGREE, 0L).intValue();
        int abstain = resultCounts.getOrDefault(VoteResultType.ABSTAIN, 0L).intValue();
        int absent = resultCounts.getOrDefault(VoteResultType.ABSENT, 0L).intValue();

        int total = agree + disagree + abstain + absent;
        double agreeRate = total > 0 ? (double) agree / total * 100 : 0.0;
        double participationRate = total > 0 ? (double) (agree + disagree + abstain) / total * 100 : 0.0;
        return new VoteResultStats(agree, disagree, abstain, absent, agreeRate, participationRate);
    }


    /**
     * 법안 통과율 계산
     * @param bills 법안 리스트
     * @return 법안 통계 정보
     */
    public BillPassStats calculateBillStats(List<ProposedBill> bills) {
        if (bills == null || bills.isEmpty()) {
            return new BillPassStats(0, 0, 0.0);
        }

        int total = bills.size();
        int passed = (int) bills.stream()
                .filter(bill -> bill.getBillStatus() != null)
                .filter(bill -> bill.getBillStatus().isPassed())
                .count();

        double passRate = (double) passed / total * 100;
        return new BillPassStats(total, passed, passRate);
    }


    /**
     * 발언에서 주요 입장 분석
     * @param statements 발언 리스트
     * @return 주요 입장 요약
     */
    public String analyzeMainStance(List<StatementDocument> statements) {
        if (statements == null || statements.isEmpty()) {
            return "입장 정보 없음";
        }

        List<String> importantSentences = statements.stream()
                .filter(s -> s.getContent() != null && !s.getContent().trim().isEmpty())
                .sorted(Comparator.comparing(StatementDocument::getStatementDate).reversed())
                .limit(3)
                .map(StatementDocument::getContent)
                .map(this::extractImportantSentence)
                .filter(sentence -> !sentence.isEmpty())
                .collect(Collectors.toList());


        if (importantSentences.isEmpty()) {
            return "입장 정보 없음";
        }

        String combinedStance = String.join(" ", importantSentences);
        return summarizeText(combinedStance, 150);
    }


    /**
     * 텍스트에서 중요한 문장 추출
     * @param text 원본 텍스트
     * @return 중요 문장
     */
    private String extractImportantSentence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String[] sentences = text.split("[.!?]\\s+");

        for (String sentence : sentences) {
            if (containsImportantKeywords(sentence)) {
                return sentence.trim() + ".";
            }
        }
        return sentences.length > 0 ? sentences[0].trim() + "." : "";
    }


    /**
     * 문장에 중요 키워드가 포함되어 있는지 확인
     * @param sentence 검사할 문장
     * @return 중요 키워드 포함 여부
     */
    private boolean containsImportantKeywords(String sentence) {

        Set<String> importantKeywords = Set.of(
                "주장", "입장", "생각", "의견", "제안", "해결", "방안",
                "정책", "개선", "필요", "중요", "반대", "찬성"
        );

        return importantKeywords.stream()
                .anyMatch(sentence::contains);
    }


    /**
     * 마지막 완전한 문장의 끝 위치 찾기
     * @param text 텍스트
     * @param maxLength 최대 길이
     * @return 문장 끝 위치
     */
    private int findLastSentenceEnd(String text, int maxLength) {
        int lastSentenceEnd = -1;
        String[] sentenceEnders = {".","!","?"};

        for (String ender : sentenceEnders) {
            int pos = text.lastIndexOf(ender, maxLength - 3);
            if (pos > lastSentenceEnd) {
                lastSentenceEnd = pos;
            }
        }
        return lastSentenceEnd;
    }
}
