package com.example.GateStatus.domain.comparison.service.request;

import com.example.GateStatus.domain.comparison.ComparisonType;

import java.time.LocalDate;
import java.util.List;

public record ComparisonRequest(
        List<Long> figureIds,
        String issueId,
        String category,
        LocalDate startDate,
        LocalDate endDate,
        List<ComparisonType> comparisonTypes
) {}
