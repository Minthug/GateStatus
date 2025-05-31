package com.example.GateStatus.domain.comparison.service;

import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;

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
    @Cacheable(value = "keywordAnalysis", key = "#contents.hashCode() + '_' + #limit")
    public Map<String, Integer> analyzeKeywords(List<String> contents, int limit) {
        if (contents == null || contents.isEmpty()) {
            return new LinkedHashMap<>();
        }

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
    public Map<String, Integer> analyzeKeywords(List<StatementDocument> statements, int limit) {
        List<String> contents = statements.stream()
                .map(StatementDocument::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return analyzeKeywords(contents, limit);
    }


}
