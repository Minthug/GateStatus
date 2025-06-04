package com.example.GateStatus.domain.comparison.controller;

import com.example.GateStatus.domain.comparison.service.ComparisonService;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/compares")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * 정치인 비교 실행 ID 기반
     * 비교 요청을 받아 결과 반환
     */
    @PostMapping
    public ResponseEntity<ComparisonResult> createComparison(@RequestBody ComparisonRequest request) {
        log.info("정치인 비교 요청: 정치인 {}명", request.figureIds().size());

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 통합 정치인 비교 API
     * ID, 이름, 혼합 모든 방식을 하나의 API로 처리
     */
    @PostMapping
    public ResponseEntity<ComparisonResult> compare(@RequestBody ComparisonRequest request) {
        String requestType = request.isMixed() ? "혼합" :
                request.hasNames() ? "이름" : "ID";

        log.info("{} 기반 정치인 비교 요청: 총 {}명", requestType, request.getTotalFigureCount());

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

        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }
}
