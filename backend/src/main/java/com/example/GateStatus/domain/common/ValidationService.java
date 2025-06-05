package com.example.GateStatus.domain.common;

import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.issue.IssueCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final FigureRepository figureRepository;

    // ===== 정치인 관련 검증 =====
    public void validateFigureId(Long figureId, String context) {
        if (figureId == null) {
            throw new IllegalArgumentException(context + ": 정치인 ID는 필수 입니다");
        }

        if (figureId <= 0) {
            throw new IllegalArgumentException(context + ": 정치인 ID는 1 이상이어야 합니다 - " + figureId);
        }

        if (figureId > 999999L) {
            throw new IllegalArgumentException(context + ": 유효하지 않은 정치인 ID 입니다 - " + figureId);
        }

        if (!figureRepository.existsById(figureId)) {
            throw new IllegalArgumentException(context + ": 존재하지 않은 정치인입니다 -" + figureId);
        }
    }

    public void validateFigureIdList(List<Long> figureIds, int minCount, int maxCount,
                                     String context, boolean checkExistence) {
        if (figureIds == null || figureIds.isEmpty()) {
            throw new IllegalArgumentException(context + ": 정치인은 최소 " + minCount + "명 이상 선택해야 합니다");
        }

        if (figureIds.size() < minCount) {
            throw new IllegalArgumentException(context + ": 정치인은 최소 " + minCount + "명 이상이어야 합니다 (현재: " + figureIds.size() + "명)");
        }

        if (figureIds.size() > maxCount) {
            throw new IllegalArgumentException(
                    context + ": 정치인은 최대 " + maxCount + "명까지 가능합니다 (현재: " + figureIds.size() + "명)");
        }

        // 각 ID 개별 검증 (DB 체크 제외)
        for (int i = 0; i < figureIds.size(); i++) {
            Long figureId = figureIds.get(i);
            if (figureId == null || figureId <= 0) {
                throw new IllegalArgumentException(
                        context + ": " + (i + 1) + "번째 정치인 ID가 유효하지 않습니다 - " + figureId);
            }
        }
        // 중복 체크
        Set<Long> uniqueIds = new HashSet<>(figureIds);
        if (uniqueIds.size() != figureIds.size()) {
            Set<Long> duplicates = findDuplicates(figureIds);
            throw new IllegalArgumentException(context + ": 중복된 정치인 ID가 있습니다 - " + duplicates);
        }

        // DB 존재 여부 확인 (옵션)
        if (checkExistence) {
            List<Figure> existingFigures = figureRepository.findAllById(figureIds);
            if (existingFigures.size() != figureIds.size()) {
                Set<Long> existingIds = existingFigures.stream().map(Figure::getId).collect(Collectors.toSet());
                Set<Long> missingIds = figureIds.stream()
                        .filter(id -> !existingIds.contains(id))
                        .collect(Collectors.toSet());
                throw new IllegalArgumentException(context + ": 존재하지 않는 정치인 ID가 있습니다 - " + missingIds);
            }
        }

        log.debug("{}: 정치인 ID 목록 검증 완료 - {}명", context, figureIds.size());
    }

    /**
     * 대시보드용 정치인 ID 목록 검증 (2-5명, DB 체크 안함)
     */
    public void validateDashboardFigureIds(List<Long> figureIds) {
        validateFigureIdList(figureIds, 2, 5, "대시보드 비교", false);
    }

    /**
     * 비교분석용 정치인 ID 목록 검증 (1-10명, DB 체크 함)
     */
    public void validateComparisonFigureIds(List<Long> figureIds) {
        validateFigureIdList(figureIds, 1, 10, "비교분석", true);
    }

    // ===== 날짜 관련 검증 =====

    /**
     * 날짜 범위 유효성 검증
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @param context 검증 컨텍스트
     * @param maxYears 최대 허용 기간(년)
     * @throws IllegalArgumentException 유효하지 않은 날짜 범위인 경우
     */
    public void validateDateRange(LocalDate startDate, LocalDate endDate, String context, int maxYears) {
        // null 체크 (둘 다 null이면 허용)
        if (startDate == null && endDate == null) {
            return;
        }

        if (startDate != null && endDate == null) {
            throw new IllegalArgumentException(context + ": 시작일이 있으면 종료일도 필수입니다");
        }

        if (startDate == null && endDate != null) {
            throw new IllegalArgumentException(context + ": 종료일이 있으면 시작일도 필수입니다");
        }

        // 기본 날짜 순서 체크
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    context + ": 시작일이 종료일보다 늦을 수 없습니다 (시작일: " + startDate + ", 종료일: " + endDate + ")");
        }

        // 과거 날짜 제한
        LocalDate minDate = LocalDate.of(2000, 1, 1);
        if (startDate.isBefore(minDate)) {
            throw new IllegalArgumentException(context + ": 시작일은 2000년 1월 1일 이후여야 합니다 - " + startDate);
        }

        // 미래 날짜 제한
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today)) {
            throw new IllegalArgumentException(context + ": 시작일은 현재 날짜 이후일 수 없습니다 - " + startDate);
        }

        if (endDate.isAfter(today)) {
            throw new IllegalArgumentException(context + ": 종료일은 현재 날짜 이후일 수 없습니다 - " + endDate);
        }

        // 기간 제한
        long yearsBetween = ChronoUnit.YEARS.between(startDate, endDate);
        if (yearsBetween > maxYears) {
            throw new IllegalArgumentException(
                    context + ": 조회 기간은 최대 " + maxYears + "년까지 가능합니다 (현재: " + yearsBetween + "년)");
        }

        log.debug("{}: 날짜 범위 검증 완료 - {} ~ {} ({}일)", context, startDate, endDate,
                ChronoUnit.DAYS.between(startDate, endDate));
    }

    /**
     * 대시보드용 날짜 범위 검증 (최대 5년)
     */
    public void validateDashboardDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate, "대시보드", 5);
    }

    /**
     * 비교분석용 날짜 범위 검증 (최대 5년)
     */
    public void validateComparisonDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate, "비교분석", 5);
    }

    // ===== 텍스트 관련 검증 =====

    /**
     * 정치인 이름 유효성 검증
     */
    public void validateFigureName(String name, String context) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(context + ": 정치인 이름은 필수입니다");
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < 2) {
            throw new IllegalArgumentException(context + ": 정치인 이름은 최소 2자 이상이어야 합니다 - " + trimmedName);
        }

        if (trimmedName.length() > 10) {
            throw new IllegalArgumentException(context + ": 정치인 이름은 최대 10자까지 가능합니다 - " + trimmedName);
        }

        if (!trimmedName.matches("^[가-힣a-zA-Z0-9\\s]+$")) {
            throw new IllegalArgumentException(context + ": 정치인 이름에 허용되지 않는 문자가 포함되어 있습니다 - " + trimmedName);
        }
    }

    /**
     * 카테고리 코드 유효성 검증
     */
    public void validateCategoryCode(String categoryCode, String context) {
        if (categoryCode != null && !categoryCode.trim().isEmpty()) {
            try {
                IssueCategory.fromCode(categoryCode.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(context + ": 유효하지 않은 카테고리 코드 - " + categoryCode);
            }
        }
    }

    // ===== 페이지네이션 검증 =====

    /**
     * 페이지네이션 파라미터 검증
     */
    public void validatePagination(Integer page, Integer size, String context) {
        if (page != null && page < 0) {
            throw new IllegalArgumentException(context + ": 페이지 번호는 0 이상이어야 합니다 - " + page);
        }

        if (size != null) {
            if (size <= 0) {
                throw new IllegalArgumentException(context + ": 페이지 크기는 1 이상이어야 합니다 - " + size);
            }

            if (size > 100) {
                throw new IllegalArgumentException(context + ": 페이지 크기는 최대 100까지 가능합니다 - " + size);
            }
        }
    }

    // ===== 복합 검증 메서드 =====

    /**
     * 대시보드 요청 전체 검증
     */
    public void validateDashboardRequest(Long figureId, String figureName,
                                         LocalDate startDate, LocalDate endDate) {
        if (figureId != null) {
            validateFigureId(figureId, "대시보드");
        }

        if (figureName != null) {
            validateFigureName(figureName, "대시보드");
        }

        if (startDate != null || endDate != null) {
            validateDashboardDateRange(startDate, endDate);
        }
    }

    /**
     * 비교분석 요청 전체 검증
     */
    public void validateComparisonRequest(ComparisonRequest request) {
        validateComparisonFigureIds(request.figureIds());
        validateComparisonDateRange(request.startDate(), request.endDate());
        validateCategoryCode(request.category(), "비교분석");

        // 이슈 ID 검증 (추가 로직)
        if (request.issueId() != null && !request.issueId().trim().isEmpty()) {
            // 이슈 존재 여부 확인 로직 추가 가능
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 리스트에서 중복 요소 찾기
     */
    private Set<Long> findDuplicates(List<Long> list) {
        Set<Long> seen = new HashSet<>();
        return list.stream()
                .filter(item -> !seen.add(item))
                .collect(Collectors.toSet());
    }
}
