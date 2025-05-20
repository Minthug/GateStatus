package com.example.GateStatus.domain.dashboard.controller;

import com.example.GateStatus.domain.dashboard.dto.response.DashboardResponse;
import com.example.GateStatus.domain.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

//    @GetMapping("/figure/{figureId}")
//    public ResponseEntity<DashboardResponse> getFigureDashboard(@PathVariable String figureId) {
//
//
//        log.info("대시보드 정보 조회 요청: figureId={}", figureId);
//        DashboardResponse response = dashboardService.getDashboardData(figureId);
//
//        return ResponseEntity.ok(response);
//    }

    /**
     * 정치인 이름으로 대시보드 데이터 조회
     */
    @GetMapping("/figure/name")
    public ResponseEntity<DashboardResponse> getFigureDashboardByName(@RequestParam String name) {
        log.info("이름으로 대시보드 정보 조회 요청: name={}", name);
        DashboardResponse response = dashboardService.getDashboardDataByName(name);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/compare")
//    public ResponseEntity<List<DashboardResponse>> compareFigures(@RequestParam String figureId1,
//                                                                  @RequestParam String figureId2) {
//        log.info("정치인 비교 대시보드 요청: figureId1={}, figureId2={}", figureId1, figureId2);
//
//        List<DashboardResponse> responses = dashboardService.getComparisonData(figureId1, figureId2);
//        return ResponseEntity.ok(responses);
//    }
}
