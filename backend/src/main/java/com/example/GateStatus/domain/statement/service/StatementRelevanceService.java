package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatementRelevanceService {

    private final StatementMongoRepository statementMongoRepository;
    private final FigureRepository figureRepository;


    @Transactional(readOnly = true)
    public Page<StatementResponse> findStatementsByRelevance(String figureName, Pageable pageable) {
        log.info("인물 '{}' 관련 발언 조회 및 관련성 분석 시작", figureName);

        // 1. 인물 정보 조회
        Figure figure = figureRepository.findByName(figureName)
                .orElseThrow(() -> new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName));

        // 2. 발언 조회
        Page<StatementDocument> statementsPage = statementMongoRepository.findByFigureId(figure.getId(), pageable);
        List<StatementDocument> statements = statementsPage.getContent();
        log.info("인물 '{}' 관련 발언 {}건 조회됨", figureName, statements.size());

        // 3. 관련성 점수 계산 및 정렬
        List<ScoredStatement> scoredStatements = statements.stream()
                .map(statement -> new ScoredStatement(statement, calculateRelevanceScore(statement, figureName)))
                .sorted((s1, s2) -> Double.compare(s2.getScore(), s1.getScore())) // 내림차순 정렬
                .collect(Collectors.toList());


        // 4. 로깅 (디버깅용)
        if (log.isDebugEnabled()) {
            scoredStatements.forEach(scored ->
                    log.debug("발언 '{}' 관련성 점수: {}", scored.getStatement().getTitle(), scored.getScore()));
        }

        // 5. 점수 기준으로 정렬된 발언 목록 생성
        List<StatementDocument> sortedDocuments = scoredStatements.stream()
                .map(ScoredStatement::getStatement)
                .collect(Collectors.toList());

        // 6. 응답 변환
        List<StatementResponse> responseList = sortedDocuments.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, scoredStatements.size());

    }

    private Double calculateRelevanceScore(StatementDocument statement, String figureName) {
        double score = 0.0;

        // 1. 제목에 인물 이름이 포함된 경우 가중치 부여
        if (statement.getTitle() != null && statement.getTitle().contains(figureName)) {
            score += 5.0;

            // 제목 시작 부분에 인물 이름이 있으면 추가 가중치
            if (statement.getTitle().startsWith(figureName)) {
                score += 5.0;
            }
        }

        // 2. 내용 분석
        if (statement.getContent() != null) {
            String content = statement.getContent();

            // 인물 이름 등장 빈도
            int nameCount = countOccurrences(content, figureName);
            score += nameCount * 1.0;

            // 직함과 함께 언급된 경우 (더 높은 관련성)
            int nameWithTitleCount = countOccurrences(content, figureName + " 의원") +
                    countOccurrences(content, figureName + " 대표") +
                    countOccurrences(content, figureName + " 위원장") +
                    countOccurrences(content, figureName + "의원") +
                    countOccurrences(content, figureName + "대표");
            score += nameWithTitleCount * 2.0;

            if ((content.contains(figureName + "는 ") || content.contains(figureName + " ")) &&
                    (content.contains("말했") || content.contains("주장했") || content.contains("강조했") || content.contains("밝혔"))) {
                score += 10.0;
            }

            // 4. 첫 문장에 언급된 경우 (중요도 높음)
            String firstSentence = getFirstSentence(content);
            if (firstSentence.contains(figureName)) return score += 3.0;
        }

        return score;
    }


    /**
     * 텍스트의 첫 문장을 추출
     */
    private String getFirstSentence(String text) {
        int endIndex = text.indexOf('.');
        return endIndex > 0 ? text.substring(0, endIndex + 1) : text;
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }

        return count;
    }

    public boolean isMainSpeaker(StatementDocument document, String figureName) {
        double relevanceScore = calculateRelevanceScore(document, figureName);
        return relevanceScore >= 10.0;
    }

    public static class ScoredStatement {
        private final StatementDocument statementDocument;
        private final double score;

        public ScoredStatement(StatementDocument statementDocument, double score) {
            this.statementDocument = statementDocument;
            this.score = score;
        }

        public StatementDocument getStatement() {
            return statementDocument;
        }

        public double getScore() {
            return score;
        }
    }
}
