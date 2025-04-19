package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMigrationService {

    private final StatementRepository statementRepository;
    private final StatementService statementService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void migrateOnStartUp() {
        log.info("애플리케이션 시작 시 데이터 마이그레이션 실행 중...");
        migrateStatements();
    }

    @Transactional(readOnly = true)
    public void migrateStatements() {
        List<Statement> statements = statementRepository.findAll();

        if (statements.isEmpty()) {
            log.info("마이그레이션할 발언 데이터가 없습니다");
            return;
        }

        log.info("JPA에서 MongoDB로 {}개의 발언 데이터 마이그레이션 시작", statements.size());
        statementService.migrateFromJpa(statements);
        log.info("데이터 마이그레이션 완료");
    }
}
