package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.StatementType;
import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.plaf.nimbus.State;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/statements")
@RequiredArgsConstructor
@Slf4j
public class StatementController {

    private final StatementService statementService;

    /**
     * 발언 ID로 발언 상세 정보 조회
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatementResponse> getStatementById(@PathVariable Long id) {
        log.info("발언 상세 정보 조회 요청: {}", id);
        StatementResponse statement = statementService.findStatementById(id);
        return ResponseEntity.ok(statement);
    }

    /**
     * 특정 정치인의 발언 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @GetMapping("/{figureId}")
    public ResponseEntity<Page<StatementResponse>> getStatementsByFigure(@PathVariable Long figureId,
                                                                         @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인별 발언 목록 조회 요청: {}", figureId);
        Page<StatementResponse> statements = statementService.findStatementsByFigure(figureId, pageable);
        return ResponseEntity.ok(statements);
    }

    /**
     * 인기 발언 목록 조회
     * @param limit
     * @return
     */
    @GetMapping("/popular")
    public ResponseEntity<List<StatementResponse>> getPopularStatements(@RequestParam(defaultValue = "10") int limit) {
        log.info("인기 발언 목록 조회 요청: 상위 {}", limit);
        List<StatementResponse> statements = statementService.findPopularStatements(limit);
        return ResponseEntity.ok(statements);
    }

    /**
     * 키워드로 발언 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<Page<StatementResponse>> searchStatements(@RequestParam String keyword,
                                                                    @PageableDefault(size = 10) Pageable pageable) {
        log.info("발언 검색 요청: keyword = {}", keyword);
        Page<StatementResponse> statements = statementService.searchStatements(keyword, pageable);
        return ResponseEntity.ok(statements);
    }

    /**
     * 특정 유형의 발언 목록 조회
     * @param type
     * @return
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<StatementResponse>> getStatementsByType(@PathVariable StatementType type) {
        log.info("유형별 발언 목록 조회 요청: {}", type);
        List<StatementResponse> statements = statementService.findStatementsByType(type);
        return ResponseEntity.ok(statements);
    }

    /**(
     * 기간별 발언 목록 조회
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/period")
    public ResponseEntity<List<StatementResponse>> getStatementsByPeriod(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("기간별 발언 목록 조회 요청: {} ~ {}", startDate, endDate);
        List<StatementResponse> statements = statementService.findStatementsByPeriod(startDate, endDate);
        return ResponseEntity.ok(statements);
    }
}
