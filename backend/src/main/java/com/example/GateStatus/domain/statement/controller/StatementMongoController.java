package com.example.GateStatus.domain.statement.controller;


import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.request.FactCheckRequest;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.open.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/statements")
@Slf4j
public class StatementMongoController {

    private final StatementService statementService;
    
    @GetMapping("/detail/{id}")
    public ResponseEntity<StatementResponse> getStatementById(@PathVariable String id) {
        log.info("[MongoDB] 발언 상세 정보 조회 요청: {}", id);
        StatementResponse response = statementService.findStatementById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-figure/{figureId}")
    public ResponseEntity<Page<StatementResponse>> getStatementByFigure(@PathVariable Long figureId,
                                                                        @PageableDefault(size = 10)Pageable pageable) {
        log.info("[MongoDB] 정치인별 발언 목록 조회 요청: {}", figureId);
        Page<StatementResponse> responses = statementService.findStatementsByFigure(figureId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-figure-name")
    public ResponseEntity<Page<StatementResponse>> getStatementByFigureName(@RequestParam String figureName,
                                                                            @PageableDefault(size = 10) Pageable pageable) {
        log.info("[MongoDB] 정치인 이름으로 발언 목록 조회 요청: {}", figureName);
        Page<StatementResponse> responses = statementService.findStatementsByFigureName(figureName, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<StatementResponse>> getPopularStatements(@RequestParam(defaultValue = "10") int limit) {
        log.info("[MongoDB] 인기 발언 목록 조회 요청: 상위 {}", limit);
        List<StatementResponse> responses = statementService.findPopularStatements(limit);
        return ResponseEntity.ok(responses);
    }

//    @GetMapping("/search")
//    public ResponseEntity<Page<StatementResponse>> searchStatements(@RequestParam String keyword,
//                                                                    @PageableDefault(size = 10) Pageable pageable) {
//        log.info("[MongoDB] 발언 검색 요청: 키워드 = {}", keyword);
//        Page<StatementResponse> responses = statementService.searchStatements(keyword, pageable);
//        return ResponseEntity.ok(responses);
//    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<StatementResponse>> getStatementsByType(@PathVariable StatementType type) {
        log.info("[MongoDB] 유형별 발언 목록 조회 요청: {}", type);
        List<StatementResponse> responses = statementService.findStatementsByType(type);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-period")
    public ResponseEntity<List<StatementResponse>> getStatementsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("[MongoDB] 기간별 발언 목록 조회 요청: {} ~ {}", startDate, endDate);
        List<StatementResponse> responses = statementService.findStatementsByPeriod(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{source}")
    public ResponseEntity<List<StatementResponse>> getStatementsBySource(@PathVariable String source) {
        log.info("[MongoDB] 출처별 발언 목록 조회 요청: {}", source);
        List<StatementResponse> responses = statementService.findStatementsBySource(source);
        return ResponseEntity.ok(responses);
    }

//    @GetMapping("/by-keyword/{keyword}")
//    public ResponseEntity<List<StatementResponse>> getStatementsByKeyword(@PathVariable String keyword) {
//        log.info("[MongoDB] 키워드 포함 발언 검색 요청: {}", keyword);
//        List<StatementResponse> responses = statementService.searchStatementContent(keyword);
//        return ResponseEntity.ok(responses);
//    }

    @PostMapping
    public ResponseEntity<StatementResponse> addStatement(@Valid @RequestBody StatementRequest request) {
        log.info("[MongoDB] 새 발언 추가 요청: 정치인 ID = {}, 제목 = {}", request.figureId(), request.title());
        StatementResponse statement = statementService.addStatement(request);
        return ResponseEntity.ok(statement);
    }

    // 발언 팩트체크 수동 업데이트
//    @PutMapping("/{id}/fact-check")
//    public ResponseEntity<StatementResponse> updateFactCheck(@PathVariable String id,
//                                                             @Valid @RequestBody FactCheckRequest request) {
//        log.info("[MongoDB] 발언 팩트체크 업데이트 요청: ID = {}, 점수 = {}", id, request.score());
//        StatementResponse response = statementService.updateFactCheck(id, request);
//        return ResponseEntity.ok(response);
//    }

//    @PostMapping("/{id}/auto-fact-check")
//    public ResponseEntity<ApiResponse<StatementResponse>> performAutoFactCheck(@PathVariable String id) {
//        log.info("[MongoDB] 발언 자동 팩트체크 요청: ID = {}", id);
//        try {
//            StatementResponse response = statementService.performFactCheck(id);
//            return ResponseEntity.ok(ApiResponse.success("팩트체크가 성공적으로 수행되었습니다", response));
//        } catch (Exception e) {
//            log.error("[MongoDB] 발언 자동 팩트체크 실패: {}", e.getMessage(), e);
//            return ResponseEntity.ok(ApiResponse.error("팩트체크 처리 중 오류 발생: " + e.getMessage()));
//        }
//    }

    @GetMapping("/fact-check")
    public ResponseEntity<List<StatementResponse>> getStatementsByFactCheckScore(@RequestParam(defaultValue = "70") Integer minScore) {
        log.info("[MongoDB] 팩트체크 점수 기준 발언 목록 조회 요청: 최소 점수 = {}", minScore);
        List<StatementResponse> responses = statementService.findStatementsFactCheckScore(minScore);
        return ResponseEntity.ok(responses);
    }
}
