package com.example.GateStatus.domain.comparison.controller;

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
import java.util.List;

@RestController
@RequestMapping("/v1/compares")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * 여러 정치인을 비교 (POST 방식)
     * 가장 일반적인 비교 요청을 처리
     * 모든 비교 옵션을 JSON으로 한 번에 전송
     * 복잡한 요청에 적합
     */
    @PostMapping
    public ResponseEntity<ComparisonResult> compareMultipleFigures(@RequestBody ComparisonRequest request) {
        log.info("정치인 비교 요청: 정치인 IDs={}, 이슈={}", request.figureIds(), request.issueId());
        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 두 정치인 비교 (GET 방식)
     * 두 정치인을 비교하는 간단한 GET 요청
     * 쿼리 파라미터로 모든 옵션 전달
     * 사용자가 URL을 공유하기 쉽게 설계됨
     */
    @GetMapping("/figures")
    public ResponseEntity<ComparisonResult> compareWithFigures(@RequestParam Long figureId1,
                                                               @RequestParam Long figureId2,
                                                               @RequestParam(required = false) String issueId,
                                                               @RequestParam(required = false) String category,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                               @RequestParam(required = false) List<ComparisonType> types) {
        log.info("두 정치인 비교 요청: 정치인1={}, 정치인2={}, 이슈={}", figureId1, figureId2, issueId);

        ComparisonRequest request = new ComparisonRequest(
                List.of(figureId1, figureId2),
                issueId,
                category,
                startDate,
                endDate,
                types
        );

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 이슈에 대한 정치인들의 입장 비교
     * 특정 이슈에 초점을 맞춘 비교
     * URL 경로에 이슈 ID 포함 (예: /comparisons/{issueId}/figures)
     * 이슈 중심 API 설계
     */
    @GetMapping("/{issueId}/figures")
    public ResponseEntity<ComparisonResult> compareOnIssue(@PathVariable String issueId,
                                                           @RequestParam List<Long> figureIds,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("이슈별 정치인 비교 요청: 이슈={}, 정치인={}", issueId, figureIds);

        ComparisonRequest request = new ComparisonRequest(
                figureIds,
                issueId,
                null,
                startDate,
                endDate,
                List.of(ComparisonType.STATEMENT, ComparisonType.VOTE, ComparisonType.BILL)
        );

        ComparisonResult result = comparisonService.compareByIssue(request);

        return ResponseEntity.ok(result);
    }


    /**
     * 특정 카테고리 내 정치인들의 활동 비교
     * 특정 카테고리 내 비교
     * URL 경로에 카테고리 포함 (예: /comparisons/{category}/figures)
     * 카테고리 중심 API 설계
     */
    @GetMapping("/{category}/figures")
    public ResponseEntity<ComparisonResult> compareInCategory(@PathVariable String category,
                                                              @RequestParam List<Long> figureIds,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("카테고리별 정치인 비교 요청: 카테고리={}, 정치인={}", category, figureIds);

        ComparisonRequest request = new ComparisonRequest(
                figureIds,
                null,
                category,
                startDate,
                endDate,
                List.of(ComparisonType.STATEMENT, ComparisonType.VOTE, ComparisonType.BILL)
        );

        ComparisonResult result = comparisonService.compareByIssue(request);

        return ResponseEntity.ok(result);
    }
}
