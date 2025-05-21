package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.service.StatementApiService;
import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final StatementService statementService;
    private final RedisTemplate redisTemplate;

    @GetMapping("/politician/name")
    public ResponseEntity<List<StatementResponse>> getStatementsByPolitician(@RequestParam String name) {
       String cacheKey = "statements:politician:" + name;

        log.info("정치인 '{}' 발언 조회 시작", name);

        cacheService.delete(cacheKey);

       List<StatementResponse> statements = cacheService.getOrSet(cacheKey, () -> apiService.getStatementsByPolitician(name),
               3600);

       return ResponseEntity.ok(statements);
    }
}
