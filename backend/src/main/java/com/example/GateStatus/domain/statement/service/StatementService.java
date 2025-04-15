package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.repository.StatementRepository;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final StatementRepository statementRepository;
    private final FigureRepository figureRepository;
    private final StatementApiService apiService;

    @Transactional
    public StatementResponse findStatementById(Long id) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        statement.incrementViewCount();
        return StatementResponse.from(statement);
    }
}
