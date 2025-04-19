package com.example.GateStatus.domain.statement.controller;


import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/statements")
@Slf4j
public class StatementMongoController {

    private final StatementService statementService;
    
    @GetMapping
    public ResponseEntity<StatementResponse> getStatementById(@PathVariable String id) {
        log.info("[MongoDB] 발언 상세 정보 조회 요청: {}", id);
        StatementResponse response = statementService.findStatementById(id);
        return ResponseEntity.ok(response);
    }
}
