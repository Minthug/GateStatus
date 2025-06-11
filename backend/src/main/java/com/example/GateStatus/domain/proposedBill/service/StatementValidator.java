package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementSearchCriteria;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Component
public class StatementValidator {

    private static final int MAX_LIMIT = 1000;
    private static final int MIN_FACT_CHECK_SCORE = 0;
    private static final int MAX_FACT_CHECK_SCORE = 100;
    private static final int MAX_CONTENT_LENGTH = 50000;

    public void validateStatementId(String statementId) {
        if (!StringUtils.hasText(statementId)) {
            throw new IllegalArgumentException("발언 ID는 필수 입니다");
        }
    }

    public void validateFigureId(Long figureId) {

    }

    public void validateFigureName(String figureName) {
    }

    public void validateStatementType(StatementType type) {

    }

    public void validateDateRange(LocalDate startDate, LocalDate endDate) {


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
}
