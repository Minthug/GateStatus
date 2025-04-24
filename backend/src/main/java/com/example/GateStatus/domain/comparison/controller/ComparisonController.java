package com.example.GateStatus.domain.comparison.controller;

import com.example.GateStatus.domain.comparison.ComparisonType;
import com.example.GateStatus.domain.comparison.service.ComparisonService;
import com.example.GateStatus.domain.comparison.service.request.ComparisonRequest;
import com.example.GateStatus.domain.comparison.service.response.ComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
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

    @PostMapping
    public ResponseEntity<ComparisonResult> compareMultipleFigures(@RequestBody ComparisonRequest request) {
        log.info("정치인 비교 요청: 정치인 IDs={}, 이슈={}", request.figureIds(), request.issueId());
        ComparisonResult result = comparisonService.compareByIssue(request);
        return ResponseEntity.ok(result);
    }

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


}
