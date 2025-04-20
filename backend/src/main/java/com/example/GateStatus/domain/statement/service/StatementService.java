package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.statement.service.request.FactCheckRequest;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final FigureRepository figureRepository;
    private final StatementApiService apiService;
    private final StatementMongoRepository statementMongoRepository;
    private final StatementApiMapper mapper;
    private final WebClient.Builder webclientBuilder;

    @Value("${spring.openapi.assembly.url}")
    private String baseUrl;

    @Value("${spring.openapi.assembly.key}")
    private String apikey;

    /**
     * 발언 ID로 발언 상세 정보 조회
     * @param id
     * @return
     */
    @Transactional
    public StatementResponse findStatementById(String id) {
        StatementDocument statement = statementMongoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        statement.incrementViewCount();
        statementMongoRepository.save(statement);

        return StatementResponse.from(statement);
    }

    /**
     * 특정 정치인이 발언 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional
    public Page<StatementResponse> findStatementsByFigure(Long figureId, Pageable pageable) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다 " + figureId));

        Page<StatementDocument> statements = statementMongoRepository.findByFigureId(figureId, pageable);
        return statements.map(StatementResponse::from);
    }

    /**
     * 인기 발언 목록 조회
     * @param limit
     * @return
     */
    @Transactional
    public List<StatementResponse> findPopularStatements(int limit) {
        return statementMongoRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, limit))
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 키워드로 발언 검색 (정규식 기반)
     * @param keyword
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> searchStatements(String keyword) {
        List<StatementDocument> statements = statementMongoRepository.findByContentContainingKeyword(keyword);
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 발언 검색 (페이징 적용)
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> searchStatements(String keyword, Pageable pageable) {
        try {
            return statementMongoRepository.fullTextSearch(keyword, pageable)
                    .map(StatementResponse::from);
        } catch (Exception e) {
            log.warn("텍스트 검색 실패, 정규식 검색으로 대체: {}", e.getMessage());
            return statementMongoRepository.searchByRegex(keyword, pageable)
                    .map(StatementResponse::from);
        }
    }

    /**
     * 특정 유형의 발언 목록 조회
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsByType(StatementType type) {
        return statementMongoRepository.findByType(type)
                .stream()
                .map(this::convertToResponse)
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
        return statementMongoRepository.findByPeriod(startDate, endDate)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 팩트체크 점수가 일정 수준 이상인 발언 목록 조회
     * @param minScore
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsFactCheckScore(Integer minScore) {
        return statementMongoRepository.findByFactCheckScoreGreaterThanEqual(minScore)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 출처의 발언 목록 조회
     * @param source
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsBySource(String source) {
        return statementMongoRepository.findBySource(source)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 새 발언 추가
     * @param request
     * @return
     */
    @Transactional
    public StatementResponse addStatement(StatementRequest request) {
        Figure figure = figureRepository.findById(request.figureId())
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + request.figureId()));

        StatementDocument statement = StatementDocument.builder()
                .figureId(figure.getId())
                .figureName(figure.getName())
                .title(request.title())
                .content(request.content())
                .statementDate(request.statementDate())
                .source(request.source())
                .context(request.context())
                .originalUrl(request.originalUrl())
                .type(request.type())
                .factCheckScore(null)
                .factCheckResult(null)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StatementDocument savedStatement = statementMongoRepository.save(statement);
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
    public StatementResponse updateFactCheck(String id, Integer score, String result) {
        StatementDocument statement = statementMongoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        statement.updateFactCheck(score, result);
        statementMongoRepository.save(statement);

        return StatementResponse.from(statement);
    }

    /**
     * 팩트체크 요청으로 업데이트 (편의 메서드)
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public StatementResponse updateFactCheck(String id, FactCheckRequest request) {
        return updateFactCheck(id, request.score(), request.result());
    }

    @Transactional
    public int syncStatementsByFigure(String figureName) {
        log.info("국회방송국 API에서 '{}' 인물 발언 정보 동기화 시작", figureName);

        Figure figure = figureRepository.findByName(figureName)
                .orElseThrow(() -> new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName));

        AssemblyApiResponse<String> apiResponse = fetchStatementsByFigure(figureName);
        if (!apiResponse.isSuccess()) {
            log.error("API 호출 실패: {}", apiResponse.resultMessage());
            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
        }

        List<StatementApiDTO> apiDtos = mapper.map(apiResponse);
        int syncCount = 0;

        for (StatementApiDTO dto : apiDtos) {
            if (statementMongoRepository.existsByOriginalUrl(dto.originalUrl())) {
                log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
                continue;
            }

            StatementDocument document = convertToResponse(dto, figure);
            statementMongoRepository.save(document);
            syncCount++;
        }

        log.info("'{}' 인물 발언 정보 동기화 완료. 총 {} 건 처리됨", figureName, syncCount);
        return syncCount;
    }

    /**
     * 기간별 API 데이터 동기화 메서드
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional
    public int syncStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("국회방송국 API에서 기간({} ~ {}) 발언 정보 동기화 시작", startDate, endDate);

        AssemblyApiResponse<String> apiResponse = fetchStatementsByPeriod(startDate, endDate);
        if (!apiResponse.isSuccess()) {
            throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
        }

        List<StatementApiDTO> apiDtos = mapper.map(apiResponse);
        int syncCount = 0;

        for (StatementApiDTO dto : apiDtos) {
            // 중복 체크 - exists 메서드 사용으로 최적화
            if (statementMongoRepository.existsByOriginalUrl(dto.originalUrl())) {
                log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
                continue;
            }

            // 발언자 확인
            Figure figure = figureRepository.findByName(dto.figureName())
                    .orElseGet(() -> {
                        Figure newFigure = Figure.builder()
                                .name(dto.figureName())
                                .build();
                        return figureRepository.save(newFigure);
                    });

            StatementDocument document = convertToStatement(dto, figure);
            statementMongoRepository.save(document);
            syncCount++;
        }

        log.info("기간({} ~ {}) 발언 정보 동기화 완료. 총 {}건 처리됨", startDate, endDate, syncCount);
        return syncCount;
    }

    private StatementDocument convertToDocument(Statement entity) {
        return StatementDocument.builder()
                .figureId(entity.getFigure().getId())
                .figureName(entity.getFigure().getName())
                .title(entity.getTitle())
                .content(entity.getContent())
                .statementDate(entity.getStatementDate())
                .source(entity.getSource())
                .context(entity.getContext())
                .originalUrl(entity.getOriginalUrl())
                .type(entity.getType())
                .factCheckScore(entity.getFactCheckScore())
                .factCheckResult(entity.getFactCheckResult())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private StatementResponse convertToResponse(StatementDocument document) {
        return new StatementResponse(
                document.getId(),
                document.getFigureId(),
                document.getFigureName(),
                document.getTitle(),
                document.getContent(),
                document.getStatementDate(),
                document.getSource(),
                document.getContext(),
                document.getOriginalUrl(),
                document.getType(),
                document.getFactCheckScore(),
                document.getFactCheckResult(),
                document.getViewCount(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }



    public void migrateFromJpa(List<Statement> statements) {
        List<StatementDocument> documents = statements.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        statementMongoRepository.saveAll(documents);
        log.info("{}개의 발언 데이터를 JPA에서 MongoDB로 마이그레이션 했습니다", documents.size());
    }
}
