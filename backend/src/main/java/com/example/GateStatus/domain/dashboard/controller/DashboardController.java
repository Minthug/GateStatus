package com.example.GateStatus.domain.dashboard.controller;

import com.example.GateStatus.domain.common.ValidationService;
import com.example.GateStatus.domain.dashboard.dto.response.DashboardResponse;
import com.example.GateStatus.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final ValidationService validationService;

    @GetMapping("/figure/{figureId}")
    public ResponseEntity<DashboardResponse> getFigureDashboardById(@PathVariable Long figureId) {
        log.info("정치인 ID로 대시보드 조회 요청: figureId={}", figureId);

        validationService.validateFigureId(figureId, "대시보드 조회");

        DashboardResponse response = dashboardService.getDashboardDataById(figureId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/figure/name/{name}")
    public ResponseEntity<DashboardResponse> getFigureDashboardByName(@PathVariable String name) {
        log.info("정치인 이름으로 대시보드 조회 요청: name={}", name);

        validationService.validateFigureName(name, "대시보드 조회");

        DashboardResponse response = dashboardService.getDashboardDataByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/figure/{figureId}/period")
    public ResponseEntity<DashboardResponse> getFigureDashboardByPeriod(
            @PathVariable Long figureId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDate endDate
            ) {

        log.info("기간별 대시보드 조회 요청: figureId={}, period={} ~ {}", figureId, startDate, endDate);

        validationService.validateDashboardRequest(figureId, null, startDate, endDate);
        DashboardResponse response = dashboardService.getDashboardDataByPeriod(figureId, startDate, endDate);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/figures/compare")
    public ResponseEntity<List<DashboardResponse>> getMultipleFigureDashboards(@RequestParam List<Long> figureIds) {
        log.info("다중 정치인 대시보드 조회 요청: figureIds={}", figureIds);

        validationService.validateDashboardFigureIds(figureIds);

        List<DashboardResponse> responses = dashboardService.getMultipleDashboardData(figureIds);
        return ResponseEntity.ok(responses);
    }


}
