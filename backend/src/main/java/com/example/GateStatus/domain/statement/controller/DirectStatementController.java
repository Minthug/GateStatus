package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.service.StatementApiService;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/statements/direct")
@RestController
public class DirectStatementController {

    private final StatementApiService apiService;
    private final RedisCacheService cacheService;

    @GetMapping("/politician")
    public ResponseEntity<List<StatementApiDTO>> getStatementsByPolitician(@RequestParam String name) {
        log.info("정치인 발언 검색: {}", name);
        List<StatementApiDTO> statements = cacheService.getOrSet(
                "statements:politician:" + name, () -> apiService.getStatementsByPolitician(name), 600);
        return ResponseEntity.ok(statements);
    }

    @GetMapping("/search")
    public ResponseEntity<List<StatementApiDTO>> searchStatements(@RequestParam(required = false) String politician,
                                                                  @RequestParam(required = false) String keyword) {
        if (politician == null && keyword == null) {
            return ResponseEntity.badRequest().body(List.of());
        }

        log.info("통합 검색: 정치인={}, 키워드={}", politician, keyword);
        String cacheKey = "statements:combined:" + (politician != null ? politician : "any") + ":" + (keyword != null ? keyword : "any");

        List<StatementApiDTO> statements = cacheService.getOrSet(cacheKey, () -> apiService.searchStatements(politician, keyword), 600);

        return ResponseEntity.ok(statements);
    }




}
