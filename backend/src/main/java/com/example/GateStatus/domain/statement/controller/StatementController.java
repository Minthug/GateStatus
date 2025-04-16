package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
