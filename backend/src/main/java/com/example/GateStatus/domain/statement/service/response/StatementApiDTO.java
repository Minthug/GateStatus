package com.example.GateStatus.domain.statement.service.response;

import java.time.LocalDate;

public record StatementApiDTO(String title, String content, LocalDate statementDate,
                              String source, String originalUrl, String typeCode, String figureName, String context) {
}
