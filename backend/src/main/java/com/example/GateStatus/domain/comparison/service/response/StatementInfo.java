package com.example.GateStatus.domain.comparison.service.response;

import java.time.LocalDate;

public record StatementInfo(
        String statementId,
        String title,
        String summary,
        LocalDate statementDate,
        String source
) {
}
