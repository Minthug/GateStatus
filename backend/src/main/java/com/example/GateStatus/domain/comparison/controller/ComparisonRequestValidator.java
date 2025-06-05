package com.example.GateStatus.domain.comparison.controller;

import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.IssueCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComparisonRequestValidator {

    private final FigureRepository figureRepository;

    public void validate(ComparisonRequest request) {
        validateFigureIds(request.figureIds());
        validateDateRange(request.startDate(), request.endDate());
        validateCategory(request.category());
    }

    private void validateFigureIds(List<Long> figureIds) {
        if (figureIds == null || figureIds.isEmpty()) {
            throw new IllegalArgumentException("비교할 정치인이 최소 1명 이상 선택해야 합니다");
        }

        if (figureIds.size() > 10) {
            throw new IllegalArgumentException("비교 가능한 정치인은 최대 10명 입니다");
        }

        Set<Long> uniqueIds = new HashSet<>(figureIds);
        if (uniqueIds.size() != figureIds.size()) {
            throw new IllegalArgumentException("중복된 정치인 ID가 포함되어 있습니다");
        }

        List<Figure> existingFigures = figureRepository.findAllById(figureIds);
        if (existingFigures.size() != figureIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 정치인 ID가 포함되어 있습니다");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작날짜는 종료날짜보다 이전이여야 합니다");
            }
            if (startDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("시작날짜는 현재날짜보다 이전이여야 합니다");
            }
            if (ChronoUnit.YEARS.between(startDate, endDate) > 5) {
                throw new IllegalArgumentException("비교 기간은 최대 5년까지 입니다");
            }
        }
    }

    private void validateCategory(String categoryCode) {
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            try {
                IssueCategory.fromCode(categoryCode);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테코리 코드: " + categoryCode);
            }
        }
    }
}
