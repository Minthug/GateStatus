package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class StatementValidator {

    private static final int MAX_LIMIT = 1000;
    private static final int MIN_LIMIT = 1;
    private static final int MIN_FACT_CHECK_SCORE = 0;
    private static final int MAX_FACT_CHECK_SCORE = 100;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MIN_CONTENT_LENGTH = 1;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_SOURCE_LENGTH = 200;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_SEARCH_KEYWORDS = 10;

    // URL 패턴 검증용 정규식
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$"
    );

    // 악성 스크립트 패턴 검증용 정규식
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
            "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*="
    );


    public void validateStatementId(String statementId) {
        if (!StringUtils.hasText(statementId)) {
            throw new IllegalArgumentException("발언 ID는 필수 입니다");
        }

        if (statementId.length() != 24 || !statementId.matches("[a-fA-F0-9]{24}")) {
            throw new IllegalArgumentException("유효하지 않은 발언 ID 형식입니다");
        }
    }

    public void validateFigureId(Long figureId) {
        if (figureId == null) {
            throw new IllegalArgumentException("정치인 ID는 필수입니다");
        }

        if (figureId <= 0) {
            throw new IllegalArgumentException("정치인 ID는 양수이어야 합니다");
        }
    }

    public void validateFigureName(String figureName) {
        if (!StringUtils.hasText(figureName)) {
            throw new IllegalArgumentException("정치인 이름은 필수 입니다");
        }

        figureName = figureName.trim();
        if (figureName.length() > 50) {
            throw new IllegalArgumentException("정치인 이름은 50자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(figureName, "정치인 이름");
    }

    public void validateLimit(int limit) {
        if (limit < MIN_LIMIT) {
            throw new IllegalArgumentException("조회 제한 수는 " + MIN_LIMIT + " 이상이어야 합니다");
        }

        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("조회 제한 수는 " + MAX_LIMIT + " 이하이어야 합니다");
        }
    }

    public void validateStatementType(StatementType type) {
        if (type == null) {
            throw new IllegalArgumentException("발언 유형은 필수입니다");
        }
    }

    public void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작 날짜와 종료 날짜는 필수 입니다");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 늦을 수 없습니다");
        }

        LocalDate maxFutureDate = LocalDate.now().minusYears(1);
        if (endDate.isAfter(maxFutureDate)) {
            throw new IllegalArgumentException("종료 날짜가 너무 먼 미래입니다");
        }

        LocalDate minPastDate = LocalDate.of(1900, 1, 1);
        if (startDate.isBefore(minPastDate)) {
            throw new IllegalArgumentException("시작 날짜가 너무 먼 과거입니다 (1900년 이후만 허용)");
        }
    }

    /**
     * 단일 날짜 유효성을 검증합니다
     * @param date 검증할 날짜
     * @param fieldName 필드명 (오류 메시지용)
     * @throws IllegalArgumentException 날짜가 유효하지 않은 경우
     */
    public void validateDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다");
        }

        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + "는 미래일 수 없습니다");
        }

        LocalDate minPastDate = LocalDate.of(1900, 1, 1);
        if (date.isBefore(minPastDate)) {
            throw new IllegalArgumentException(fieldName + "가 너무 먼 과거입니다 (1900년 이후만 허용)");
        }
    }

    public void validateSource(String source) {
        if (!StringUtils.hasText(source)) {
            throw new IllegalArgumentException("출처는 필수 입니다");
        }

        source = source.trim();
        if (source.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("출처는 " + MAX_SOURCE_LENGTH + "자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(source, "출처");
    }

    /**
     * URL 유효성을 검증합니다
     * @param url 검증할 URL
     * @param fieldName 필드명 (오류 메시지용)
     * @throws IllegalArgumentException URL이 유효하지 않은 경우
     */
    public void validateUrl(String url, String fieldName) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다");
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("유효하지 않은 " + fieldName + " 형식입니다");
        }

        if (url.length() > 2000) {
            throw new IllegalArgumentException(fieldName + "이 너무 깁니다 (최대 2000자)");
        }
    }

    public void validateSearchCriteria(StatementSearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("검색 조건은 필수 입니다");
        }

        switch (criteria.searchType()) {
            case FULL_TEXT, CONTENT_ONLY, RECENT -> validateKeyword(criteria.keyword());
            case EXACT_PHRASE -> validateExactPhrase(criteria.exactPhrase());
            case MULTIPLE_KEYWORDS -> validateMultipleKeywords(criteria.multipleKeywords());
        }
        
        if (criteria.startDate() != null && criteria.endDate() != null) {
            validateDateRange(criteria.startDate(), criteria.endDate());
        }

        validateLimit(criteria.limit());

        if (criteria.type() != null) {
            validateStatementType(criteria.type());
        }

        if (criteria.source() != null) {
            validateSource(criteria.source());
        }
    }

    public void validateContentLength(Integer minLength, Integer maxLength) {
        if (minLength != null && minLength < 0) {
            throw new IllegalArgumentException("최소 길이는 0 이상이어야 합니다");
        }
        if (maxLength != null && maxLength < 0) {
            throw new IllegalArgumentException("최대 길이는 0 이상이어야 합니다");
        }
        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw new IllegalArgumentException("최소 길이는 최대 길이보다 작거나 같아야 합니다");
        }
        if (maxLength != null && maxLength > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("최대 길이는 " + MAX_CONTENT_LENGTH + "자를 초과할 수 없습니다");
        }
    }

    /**
     * 발언 제목 유효성을 검증합니다
     * @param title 발언 제목
     * @throws IllegalArgumentException 제목이 유효하지 않은 경우
     */
    public void validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("발언 제목은 필수입니다");
        }

        title = title.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("발언 제목은 " + MAX_TITLE_LENGTH + "자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(title, "발언 제목");
    }

    /**
     * 발언 내용 유효성을 검증합니다
     * @param content 발언 내용
     * @throws IllegalArgumentException 내용이 유효하지 않은 경우
     */
    public void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("발언 내용은 필수입니다");
        }

        content = content.trim();
        if (content.length() < MIN_CONTENT_LENGTH) {
            throw new IllegalArgumentException("발언 내용은 최소 " + MIN_CONTENT_LENGTH + "자 이상이어야 합니다");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("발언 내용은 " + MAX_CONTENT_LENGTH + "자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(content, "발언 내용");
    }

    // ========== 팩트체크 관련 검증 ==========

    /**
     * 팩트체크 점수 유효성을 검증합니다
     * @param score 팩트체크 점수
     * @throws IllegalArgumentException 점수가 유효 범위를 벗어난 경우
     */
    public void validateFactCheckScore(Integer score) {
        if (score == null) {
            throw new IllegalArgumentException("팩트체크 점수는 필수입니다");
        }
        if (score < MIN_FACT_CHECK_SCORE || score > MAX_FACT_CHECK_SCORE) {
            throw new IllegalArgumentException(
                    String.format("팩트체크 점수는 %d~%d 사이여야 합니다", MIN_FACT_CHECK_SCORE, MAX_FACT_CHECK_SCORE));
        }
    }

    // ========== 복합 검증 메서드들 ==========

    /**
     * 발언 요청 정보 유효성을 검증합니다
     * @param request 발언 요청 정보
     * @throws IllegalArgumentException 필수 정보가 누락되거나 잘못된 경우
     */
    public void validateStatementRequest(StatementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("발언 요청 정보는 필수입니다");
        }

        // 개별 필드 검증
        validateFigureId(request.figureId());
        validateTitle(request.title());
        validateContent(request.content());
        validateDate(request.statementDate(), "발언 날짜");
        validateSource(request.source());
        validateUrl(request.originalUrl(), "원본 URL");

        // 발언 유형 검증 (설정된 경우)
        if (request.type() != null) {
            validateStatementType(request.type());
        }

        // 컨텍스트 검증 (설정된 경우)
        if (StringUtils.hasText(request.context())) {
            validateNoMaliciousContent(request.context(), "발언 맥락");
        }
    }

    /**
     * 마이그레이션 데이터 유효성을 검증합니다
     * @param statements JPA Statement 엔티티 목록
     * @throws IllegalArgumentException 데이터가 null이거나 비어있는 경우
     */
    public void validateMigrationData(List<com.example.GateStatus.domain.statement.entity.Statement> statements) {
        if (statements == null) {
            throw new IllegalArgumentException("마이그레이션할 발언 데이터는 필수입니다");
        }
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("마이그레이션할 발언 데이터가 없습니다");
        }
        if (statements.size() > 10000) {
            throw new IllegalArgumentException("한 번에 마이그레이션할 수 있는 데이터는 최대 10,000건입니다");
        }
    }


    private void validateNoMaliciousContent(String content, String fieldName) {
        if (MALICIOUS_PATTERN.matcher(content).find()) {
            throw new IllegalArgumentException(fieldName + "에 허용되지 않은 스크립트가 포함되어 있습니다");
        }

        String lowerContent = content.toLowerCase();
        String[] sqlKeywords = {"drop", "delete", "insert", "update", "select", "union", "exec", "script"};
        for (String keyword : sqlKeywords) {
            if (lowerContent.contains(keyword + " ") || lowerContent.contains(" " + keyword)) {
                throw new IllegalArgumentException(fieldName + "에 허용되지 않은 SQL 키워드가 포함되어 있습니다");
            }
        }
    }

    public void validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다");
        }

        keyword = keyword.trim();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("검색 키워드는 " + MAX_KEYWORD_LENGTH + "자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(keyword, "검색 키워드");
    }

    /**
     * 정확한 문구 검색 유효성을 검증합니다
     * @param phrase 검색할 문구
     * @throws IllegalArgumentException 문구가 유효하지 않은 경우
     */
    public void validateExactPhrase(String phrase) {
        if (!StringUtils.hasText(phrase)) {
            throw new IllegalArgumentException("검색할 문구는 필수입니다");
        }

        phrase = phrase.trim();
        if (phrase.length() < 2) {
            throw new IllegalArgumentException("검색 문구는 최소 2자 이상이어야 합니다");
        }
        if (phrase.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("검색 문구는 " + MAX_KEYWORD_LENGTH + "자를 초과할 수 없습니다");
        }

        validateNoMaliciousContent(phrase, "검색 문구");
    }

    /**
     * 다중 키워드 검색 유효성을 검증합니다
     * @param keywords 검색 키워드 목록
     * @throws IllegalArgumentException 키워드 목록이 유효하지 않은 경우
     */
    public void validateMultipleKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("검색 키워드 목록은 필수입니다");
        }

        if (keywords.size() < 2) {
            throw new IllegalArgumentException("다중 키워드 검색은 최소 2개 이상의 키워드가 필요합니다");
        }

        if (keywords.size() > MAX_SEARCH_KEYWORDS) {
            throw new IllegalArgumentException("검색 키워드는 최대 " + MAX_SEARCH_KEYWORDS + "개까지 가능합니다");
        }

        // 각 키워드 개별 검증
        for (String keyword : keywords) {
            validateKeyword(keyword);
        }

        // 중복 키워드 검증
        long distinctCount = keywords.stream().map(String::trim).distinct().count();
        if (distinctCount != keywords.size()) {
            throw new IllegalArgumentException("중복된 검색 키워드가 있습니다");
        }
    }
}
