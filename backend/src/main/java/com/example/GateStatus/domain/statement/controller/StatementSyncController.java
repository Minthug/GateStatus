package com.example.GateStatus.domain.statement.controller;

import com.example.GateStatus.domain.statement.service.StatementService;
import com.example.GateStatus.domain.statement.service.StatementSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/statements/sync")
public class StatementSyncController {

    private final StatementService statementService;
    private final StatementSyncService syncService;


}
