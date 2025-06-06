package com.example.GateStatus.domain.comparison.controller;

import com.example.GateStatus.domain.common.ValidationService;
import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.service.ComparisonService;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/compares")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final ValidationService validationService;

    /**
     * 정치인 비교 실행 ID 기반
     * 비교 요청을 받아 결과 반환
     */
//    @PostMapping
//    public ResponseEntity<ComparisonResult> createComparison(@RequestBody ComparisonRequest request) {
//        log.info("정치인 비교 요청: 정치인 {}명", request.figureIds().size());
//
//        validationService.validateComparisonRequest(request);
//
//        ComparisonResult result = comparisonService.compareByIssue(request);
//        return ResponseEntity.ok(result);
//    }

    /**
     * 통합 정치인 비교 API
     * ID, 이름, 혼합 모든 방식을 하나의 API로 처리
     */
    @PostMapping
    public ResponseEntity<ComparisonResult> compare(@RequestBody ComparisonRequest request) {
        String requestType = request.isMixed() ? "혼합" :
                request.hasNames() ? "이름" : "ID";

        log.info("{} 기반 정치인 비교 요청: 총 {}명", requestType, request.getTotalFigureCount());

        validationService.validateComparisonRequest(request);

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }


    /**
     * 간단한 두 정치인 비교 (GET)
     * 쿼리 파라미터로 간단 비교
     */
    @GetMapping
    public ResponseEntity<ComparisonResult> getComparison(@RequestParam(required = false) Long figureId1,
                                                          @RequestParam(required = false) Long figureId2,
                                                          @RequestParam(required = false) String figureName1,
                                                          @RequestParam(required = false) String figureName2,
                                                          @RequestParam(required = false) String issueId,
                                                          @RequestParam(required = false) String category) {

        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();

        if (figureId1 != null) ids.add(figureId1);
        if (figureId2 != null) ids.add(figureId2);
        if (figureName1 != null && !figureName1.trim().isEmpty()) names.add(figureName1.trim());
        if (figureName2 != null && !figureName2.trim().isEmpty()) names.add(figureName2.trim());

        ComparisonRequest request = new ComparisonRequest(
                ids.isEmpty() ? null : ids,
                names.isEmpty() ? null : names,
                issueId,
                category,
                null,
                null,
                null
        );

        String requestType = request.isMixed() ? "혼합" :
                request.hasNames() ? "이름" : "ID";

        log.info("간단 {} 기반 정치인 비교 요청: 총 {}명", requestType, request.getTotalFigureCount());

        validationService.validateComparisonRequest(request);

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{categoryCode}/figures")
    public ResponseEntity<ComparisonResult> compareInCategory(@PathVariable String categoryCode,
                                                              @RequestParam(required = false) List<Long> figureIds,
                                                              @RequestParam(required = false) List<String> figureNames,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("카테고리별 정치인 비교 요청: 카테고리={}, ID={}명, 이름={}명",
                categoryCode,
                figureIds != null ? figureIds.size() : 0,
                figureNames != null ? figureNames.size() : 0);

        ComparisonRequest request = new ComparisonRequest(
                figureIds,
                figureNames,
                null,        // issueId
                categoryCode,
                startDate,
                endDate,
                List.of(ComparisonType.STATEMENT, ComparisonType.VOTE, ComparisonType.BILL)
        );

        validationService.validateComparisonRequest(request);

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 정치인 비교 가능 여부 사전 검증 API
     * 실제 비교 전에 요청이 유효한지 미리 확인
     *
     * @param request 비교 요청
     * @return 검증 결과
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateComparisonRequest(@RequestBody ComparisonRequest request) {
        log.info("비교 요청 사전 검증: 총 {}명", request.getTotalFigureCount());

        try {
            validationService.validateComparisonRequest(request);

            Map<String, Object> result = Map.of(
                    "valid", true,
                    "message", "비교 요청이 유효합니다",
                    "figureCount", request.getTotalFigureCount(),
                    "requestType", request.isMixed() ? "혼합" : request.hasNames() ? "이름" : "ID"
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            Map<String, Object> result = Map.of(
                    "valid", false,
                    "message", e.getMessage(),
                    "figureCount", request.getTotalFigureCount()
            );

            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 빠른 두 정치인 비교 (경로 파라미터 방식)
     * URL에서 직접 비교 - 간단한 링크 공유에 적합
     *
     * @param figure1 첫 번째 정치인 (ID 또는 이름)
     * @param figure2 두 번째 정치인 (ID 또는 이름)
     * @return 비교 결과
     */
    @GetMapping("/quick/{figure1}/vs/{figure2}")
    public ResponseEntity<ComparisonResult> quickCompare(@PathVariable String figure1,
                                                         @PathVariable String figure2) {
        log.info("빠른 비교 요청: {} vs {}", figure1, figure2);

        // 숫자인지 문자인지 판별하여 ID/이름 구분
        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();

        // figure1 처리
        try {
            Long id1 = Long.parseLong(figure1);
            ids.add(id1);
        } catch (NumberFormatException e) {
            names.add(figure1);
        }

        // figure2 처리
        try {
            Long id2 = Long.parseLong(figure2);
            ids.add(id2);
        } catch (NumberFormatException e) {
            names.add(figure2);
        }

        ComparisonRequest request = new ComparisonRequest(
                ids.isEmpty() ? null : ids,
                names.isEmpty() ? null : names,
                null, null, null, null, null
        );

        validationService.validateComparisonRequest(request);

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 인기 비교 조합 조회
     * 자주 비교되는 정치인 조합을 반환
     *
     * @param limit 반환할 조합 수 (기본 10개)
     * @return 인기 비교 조합 목록
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularComparisons(@RequestParam(defaultValue = "10") int limit) {
        log.info("인기 비교 조합 조회 요청: limit={}", limit);

        // limit 유효성 검증
        if (limit <= 0 || limit > 50) {
            throw new IllegalArgumentException("limit은 1-50 사이여야 합니다: " + limit);
        }

        List<Map<String, Object>> popularComparisons = List.of(
                Map.of("figures", List.of("윤석열", "이재명"), "count", 1523),
                Map.of("figures", List.of("한동훈", "이준석"), "count", 892),
                Map.of("figures", List.of("김건희", "김혜경"), "count", 756)
        );

        return ResponseEntity.ok(popularComparisons.stream().limit(limit).collect(Collectors.toList()));
    }

    /**
     * 비교 히스토리 조회
     * 특정 정치인이 포함된 최근 비교 내역
     *
     * @param figureId 정치인 ID (선택사항)
     * @param figureName 정치인 이름 (선택사항)
     * @param limit 반환할 개수 (기본 20개)
     * @return 비교 히스토리 목록
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getComparisonHistory(
            @RequestParam(required = false) Long figureId,
            @RequestParam(required = false) String figureName,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("비교 히스토리 조회: figureId={}, figureName={}, limit={}", figureId, figureName, limit);

        // 파라미터 검증
        if (figureId == null && (figureName == null || figureName.trim().isEmpty())) {
            throw new IllegalArgumentException("정치인 ID 또는 이름 중 하나는 필수입니다");
        }

        if (figureId != null) {
            validationService.validateFigureId(figureId, "비교 히스토리");
        }

        if (figureName != null) {
            validationService.validateFigureName(figureName, "비교 히스토리");
        }

        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit은 1-100 사이여야 합니다: " + limit);
        }

        // TODO: 실제 히스토리 조회 로직 구현
        List<Map<String, Object>> history = List.of(
                Map.of("date", "2024-01-15", "figures", List.of("윤석열", "이재명"), "issue", "경제정책"),
                Map.of("date", "2024-01-14", "figures", List.of("윤석열", "한동훈"), "category", "정치")
        );

        return ResponseEntity.ok(history.stream().limit(limit).collect(Collectors.toList()));
    }
}
