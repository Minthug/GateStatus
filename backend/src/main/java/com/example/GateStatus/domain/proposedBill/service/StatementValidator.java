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

    public void validateSource(String source) {


    }

    public void validateSearchCriteria(StatementSearchCriteria searchCriteria) {

    }

    public void validateContentLength(Integer minLength, Integer maxLength) {


    }

    public void validateStatementRequest(StatementRequest request) {

    }

    public void validateFactCheckScore(Integer minScore) {

    }

    public void validateMigrationData(List<Statement> statements) {


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
}
