package com.example.GateStatus.domain.statement.service.response;

import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.SearchType;

import java.time.LocalDate;
import java.util.List;

public record StatementSearchCriteria(
        String keyword,
        List<String> multipleKeywords,
        String exactPhrase,
        StatementType type,
        LocalDate startDate,
        LocalDate endDate,
        String source,
        SearchType searchType,
        int limit
) {
    /**
     * 키워드 검색용 생성자
     */
    public static StatementSearchCriteria keyword(String keyword) {
        return new StatementSearchCriteria(keyword, null, null, null, null, null, null, SearchType.FULL_TEXT, 50);
    }

    /**
     * 정확한 문구 검색용 생성자
     */
    public static StatementSearchCriteria exactPhrase(String phrase) {
        return new StatementSearchCriteria(null, null, phrase, null, null, null, null, SearchType.EXACT_PHRASE, 50);
    }

    /**
     * 다중 키워드 검색용 생성자
     */
    public static StatementSearchCriteria multipleKeywords(List<String> keywords) {
        return new StatementSearchCriteria(null, keywords, null, null, null, null, null, SearchType.MULTIPLE_KEYWORDS, 50);
    }

    /**
     * 최근 발언 검색용 생성자
     */
    public static StatementSearchCriteria recent(String keyword, int limit) {
        return new StatementSearchCriteria(keyword, null, null, null, null, null, null, SearchType.RECENT, limit);
    }

    public StatementSearchCriteria withType(StatementType type) {
        return new StatementSearchCriteria(keyword, multipleKeywords, exactPhrase, type, startDate, endDate, source, searchType, limit);
    }

    /**
     * 출처 조건 추가
     */
    public StatementSearchCriteria withSource(String source) {
        return new StatementSearchCriteria(keyword, multipleKeywords, exactPhrase, type, startDate, endDate, source, searchType, limit);
    }

    /**
     * 기간 조건 추가
     */
    public StatementSearchCriteria withPeriod(LocalDate startDate, LocalDate endDate) {
        return new StatementSearchCriteria(keyword, multipleKeywords, exactPhrase, type, startDate, endDate, source, searchType, limit);
    }

    public StatementSearchCriteria withLimit(int limit) {
        return new StatementSearchCriteria(keyword, multipleKeywords, exactPhrase, type, startDate, endDate, source, searchType, limit);
    }

    /**
     * ✨ 컨트롤러용 - 파라미터에서 직접 생성
     */
    public static StatementSearchCriteria fromParams(
            String keyword, String exactPhrase, List<String> keywords,
            StatementType type, LocalDate startDate, LocalDate endDate,
            String source, Integer limit) {

        // 검색 타입 자동 결정
        SearchType searchType;
        List<String> finalKeywords = null;
        String finalKeyword = null;
        String finalExactPhrase = null;

        if (exactPhrase != null && !exactPhrase.isEmpty()) {
            searchType = SearchType.EXACT_PHRASE;
            finalExactPhrase = exactPhrase;
        } else if (keywords != null && !keywords.isEmpty()) {
            searchType = SearchType.MULTIPLE_KEYWORDS;
            finalKeywords = keywords;
        } else {
            searchType = SearchType.FULL_TEXT;
            finalKeyword = keyword != null ? keyword : "";
        }

        return new StatementSearchCriteria(
                finalKeyword, finalKeywords, finalExactPhrase, type,
                startDate, endDate, source, searchType,
                limit != null ? limit : 50
        );
    }

    /**
     * ✨ 빈 조건으로 시작 (빌더 패턴 대안)
     */
    public static StatementSearchCriteria empty() {
        return new StatementSearchCriteria(null, null, null, null, null, null, null, SearchType.FULL_TEXT, 50);
    }
}

