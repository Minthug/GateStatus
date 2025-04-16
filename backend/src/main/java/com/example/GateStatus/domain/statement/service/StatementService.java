package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.Statement;
import com.example.GateStatus.domain.statement.StatementType;
import com.example.GateStatus.domain.statement.repository.StatementRepository;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final StatementRepository statementRepository;
    private final FigureRepository figureRepository;
    private final StatementApiService apiService;

    /**
     * 발언 ID로 발언 상세 정보 조회
     * @param id
     * @return
     */
    @Transactional
    public StatementResponse findStatementById(Long id) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        statement.incrementViewCount();
        return StatementResponse.from(statement);
    }

    /**
     * 특정 정치인이 발언 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> findStatementsByFigure(Long figureId, Pageable pageable) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다 " + figureId));

        Page<Statement> statements = statementRepository.findByFigure(figure, pageable);
        return statements.map(StatementResponse::from);
    }

    /**
     * 인기 발언 목록 조회
     * @param limit
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findPopularStatements(int limit) {
        return statementRepository.findTopByOrderByViewCountDesc(PageRequest.of(0, limit))
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 발언 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> searchStatements(String keyword, Pageable pageable) {
        Page<Statement> statements = statementRepository.findByContentContainingOrTitleContaining(keyword, keyword, pageable);
        return statements.map(StatementResponse::from);
    }

    /**
     * 특정 유형의 발언 목록 조회
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsByType(StatementType type) {
        return statementRepository.findByType(type)
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 발언 목록 조회
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        return statementRepository.findByPeriod(startDate, endDate)
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 팩트체크 점수가 일정 수준 이상인 발언 목록 조회
     * @param minScore
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsFactCheckScore(Integer minScore) {
        return statementRepository.findByFactCheckScoreGreaterThanEqual(minScore)
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 출처의 발언 목록 조회
     * @param source
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsBySource(String source) {
        return statementRepository.findBySource(source)
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 새 발언 추가
     * @param request
     * @return
     */
    public StatementResponse addStatement(StatementRequest request) {
        Figure figure = figureRepository.findById(request.figureId())
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + request.figureId()));

        Statement statement = Statement.builder()
                .figure(figure)
                .title(request.title())
                .content(request.content())
                .statementDate(request.statementDate())
                .source(request.source())
                .context(request.context())
                .originalUrl(request.originalUrl())
                .type(request.type())
                .build();

        Statement savedStatement = statementRepository.save(statement);
        return StatementResponse.from(savedStatement);
    }

    /**
     * 발언에 팩트체크 결과 업데이트
     * @param id
     * @param score
     * @param result
     * @return
     */
    @Transactional
    public StatementResponse updateFactCheck(Long id, Integer score, String result) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        statement.updateFactCheck(score, result);

        return StatementResponse.from(statement);
    }

    /**
     * 기간별 API 데이터 동기화 메서드 (퍼사드 패턴)
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional
    public int syncStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        return apiService.syncStatementsByPeriod(startDate, endDate);
    }
}
