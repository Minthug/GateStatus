package com.example.GateStatus.domain.dashboard.controller;

import com.example.GateStatus.domain.common.ValidationService;
import com.example.GateStatus.domain.dashboard.dto.response.DashboardResponse;
import com.example.GateStatus.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
