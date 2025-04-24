package com.example.GateStatus.domain.comparison.service.response;

import java.util.Map;
import java.util.Objects;

public record FigureComparisonData(
        Long figureId,
        String figureName,
        String partyName,
        StatementComparisonData statements,
        VoteComparisonData votes,
        BillComparisonData bills,
        Map<String, Object> additionalData
) {
}
