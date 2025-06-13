package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.service.StatementValidator;
import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.exception.StatementNotFoundException;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.domain.statement.service.response.StatementSearchCriteria;
import com.example.GateStatus.global.openAi.OpenAiClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementMongoRepository;
    private final OpenAiClient openAiClient;
    private final StatementApiMapper apiMapper;
    private final StatementValidator validator;
    private final StatementSyncService syncService;

    /**
     * 발언 ID로 발언 상세 정보 조회
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public StatementResponse findStatementById(String id) {
        validator.validateStatementId(id);

        StatementDocument statement = findStatementDocumentById(id);

        statement.incrementViewCount();
        statementMongoRepository.save(statement);

        log.debug("발언 조회 완료: ID={}, 조회수={}", id, statement.getViewCount());
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
        validator.validateFigureId(figureId);

        figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + figureId));

        Page<StatementDocument> statements = statementMongoRepository.findByFigureId(figureId, pageable);
        log.debug("정치인별 발언 조회 완료: figureId={}, 총 개수={}", figureId, statements.getTotalElements());
        return statements.map(StatementResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<StatementResponse> findStatementsByFigureName(String figureName, Pageable pageable) {
        validator.validateFigureName(figureName);

        figureRepository.findByName(figureName)
                .orElseThrow(() -> new EntityNotFoundException("해당 이름의 정치인이 존재하지 않습니다: " + figureName));

        Page<StatementDocument> statements = statementMongoRepository.findByFigureName(figureName, pageable);
        log.debug("정치인 이름별 발언 조회 완료: figureName={}, 총 개수={}", figureName, statements.getTotalElements());

        return statements.map(StatementResponse::from);
    }

    /**
     * 인기 발언 목록 조회
     * @param limit
     * @return
     */
    @Cacheable(value = "popularStatements", key = "#limit")
    @Transactional(readOnly = true)
    public List<StatementResponse> findPopularStatements(int limit) {
        return statementMongoRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, limit))
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 유형의 발언 목록 조회
     * @param type
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsByType(StatementType type) {
        validator.validateStatementType(type);

        List<StatementDocument> statements = statementMongoRepository.findByType(type);

        log.debug("유형별 발언 조회 완료: type={}, 개수={}", type, statements.size());
        return statements.stream()
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
        validator.validateDateRange(startDate, endDate);

        List<StatementDocument> statements = statementMongoRepository.findByPeriod(startDate, endDate);

        return statements.stream()
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
        validator.validateSource(source);

        List<StatementDocument> statements = statementMongoRepository.findBySource(source);

        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    // ========== 발언 검색 메서드들 ==========

    /**
     * 키워드로 발언 검색 (페이징 적용, 제목과 내용 모두 검색 )
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> searchStatements(StatementSearchCriteria searchCriteria, Pageable pageable) {
        validator.validateSearchCriteria(searchCriteria);

        Page<StatementDocument> results = switch (searchCriteria.searchType()) {
            case FULL_TEXT -> performFullTextSearch(searchCriteria.keyword(), pageable);
            case EXACT_PHRASE -> performExactPhraseSearch(searchCriteria.exactPhrase(), pageable);
            case MULTIPLE_KEYWORDS -> performMultipleKeywordSearch(searchCriteria.multipleKeywords(), pageable);
            case CONTENT_ONLY -> performContentOnlySearch(searchCriteria.keyword(), pageable);
            case RECENT -> performRecentSearch(searchCriteria.keyword(), searchCriteria.limit(), pageable);
            default -> performFullTextSearch(searchCriteria.keyword(), pageable);
        };

        log.debug("통합 검색 완료: 검색타입={}, 총 결과={}", searchCriteria.searchType(), results.getTotalElements());
        return results.map(StatementResponse::from);
    }

    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsByContentLength(Integer minLength, Integer maxLength, Pageable pageable) {
        validator.validateContentLength(minLength, maxLength);

        List<StatementDocument> statements;

        if (maxLength != null) {
            statements = statementMongoRepository.findByContentLengthLessThan(maxLength, pageable);
            log.debug("짧은 발언 검색 완료: 최대길이={}, 개수={}", maxLength, statements.size());
        } else {
            statements = statementMongoRepository.findByContentLengthGreaterThan(minLength, pageable);
            log.debug("긴 발언 검색 완료: 최소길이={}, 개수={}", minLength, statements.size());
        }
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    // ========== 발언 관리 메서드들 ==========
    /**
     * 새 발언 추가
     */
    @Transactional
    public StatementResponse addStatement(StatementRequest request) {
        validator.validateStatementRequest(request);

        Figure figure = figureRepository.findById(request.figureId())
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + request.figureId()));

        StatementDocument statement = createStatementDocument(request, figure);
        StatementDocument savedStatement = statementMongoRepository.save(statement);

        log.info("새 발언 추가 완료: ID={}, 정치인={}", savedStatement.getId(), figure.getName());
        return StatementResponse.from(savedStatement);
    }


    @Transactional
    public int syncStatementsByFigure(String figureName) {
        validator.validateFigureName(figureName);

        return syncService.syncStatementsByFigure(figureName);
    }

    /**
     * 팩트체크 점수가 일정 수준 이상인 발언을 조회합니다
     * @param minScore 최소 팩트체크 점수
     * @return 조건에 맞는 발언 목록
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findStatementsFactCheckScore(Integer minScore) {
        validator.validateFactCheckScore(minScore);

        List<StatementDocument> statements = statementMongoRepository.findByFactCheckScoreGreaterThanEqual(minScore);
        log.debug("팩트체크 점수별 발언 조회 완료: 최소점수={}, 개수={}", minScore, statements.size());
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    public StatementDocument convertApiDtoToDocument(StatementApiDTO dto, Figure figure) {
        StatementDocument.StatementDocumentBuilder builder = StatementDocument.builder()
                .figureId(figure.getId())
                .figureName(figure.getName())
                .title(dto.title())
                .content(dto.content())
                .statementDate(dto.statementDate())
                .source(dto.source())
                .context(dto.context())
                .originalUrl(dto.originalUrl())
                .type(apiMapper.determineStatementType(dto.typeCode()))
                .viewCount(0)
                .factCheckScore(null)
                .factCheckResult(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());

        if (dto.content() != null && dto.content().length() > 50) {
            try {
                List<String> keywords = openAiClient.extractKeywords(dto.content());
                builder.topics(keywords);

                StatementType aiDeterminedType = openAiClient.classifyStatement(dto.content());
                if (aiDeterminedType != StatementType.OTHER) {
                    builder.type(aiDeterminedType);
                }

                if (dto.content().length() > 200) {
                    String summary = openAiClient.summarizeStatement(dto.content());
                    builder.summary(summary);
                }

                // 감성 분석
                Map<String, Double> sentiment = openAiClient.analyzeSentiment(dto.content());
                Map<String, Object> nlpData = new HashMap<>();
                nlpData.put("sentiment", sentiment);
                builder.nlpData(nlpData);
            } catch (Exception e) {
                log.warn("AI 분석 중 오류 발생: {}", e.getMessage());
            }
        }

        return builder.build();
    }


    /**
     * JPA Entity에서 MongoDB Document로 데이터를 마이그레이션합니다
     * @param statements JPA Statement 엔티티 목록
     */
    @Transactional
    public void migrateFromJpa(List<Statement> statements) {
        validator.validateMigrationData(statements);

        List<StatementDocument> documents = statements.stream()
                .map(this::convertJpaEntityToDocument)
                .collect(Collectors.toList());

        statementMongoRepository.saveAll(documents);
        log.info("{}개의 발언 데이터를 JPA에서 MongoDB로 마이그레이션 했습니다", documents.size());
    }

    // ========== Private Helper 메서드들 ==========

    /**
     * StatementRequest -> StatementDocument 생성
     */
    private StatementDocument createStatementDocument(StatementRequest request, Figure figure) {
        return StatementDocument.builder()
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
    }

    private StatementDocument findStatementDocumentById(String id) {
        return statementMongoRepository.findById(id)
                .orElseThrow(() -> new StatementNotFoundException(id));
    }

    /**
     * 최근 발언에서 키워드 검색을 수행합니다.
     */
    private Page<StatementDocument> performRecentSearch(String keyword, int limit, Pageable pageable) {
        List<StatementDocument> statements = statementMongoRepository.findByKeywordOrderByStatementDateDesc(
                keyword, PageRequest.of(0, limit));
        return new org.springframework.data.domain.PageImpl<>(statements, pageable, statements.size());
    }

    /**
     * 발언 내용만 검색합니다
     */
    private Page<StatementDocument> performContentOnlySearch(String keyword, Pageable pageable) {
        List<StatementDocument> statements = statementMongoRepository.findByContentContainingKeyword(keyword);
        return new org.springframework.data.domain.PageImpl<>(statements, pageable, statements.size());
    }

    /**
     * 다중 키워드 검색을 수행합니다 (AND 조건)
     */
    private Page<StatementDocument> performMultipleKeywordSearch(List<String> keywords, Pageable pageable) {
        if (keywords.size() < 2) {
            throw new IllegalArgumentException("최소 2개 이상의 키워드가 필요 합니다");
        }
        List<StatementDocument> results = statementMongoRepository.findByMultipleKeywords(keywords.get(0), keywords.get(1));

        if (keywords.size() > 2) {
            for (int i = 2; i < keywords.size(); i ++) {
                final String keyword = keywords.get(i);
                results = results.stream()
                        .filter(statement -> statement.getContent().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new org.springframework.data.domain.PageImpl<>(results, pageable, results.size());
    }

    /**
     * 정확한 문구 검색을 수행합니다
     */
    private Page<StatementDocument> performExactPhraseSearch(String phrase, Pageable pageable) {
        String escapedPhrase = java.util.regex.Pattern.quote(phrase);
        List<StatementDocument> statements = statementMongoRepository.findByExactPhraseInContent(escapedPhrase);
        return new org.springframework.data.domain.PageImpl<>(statements, pageable, statements.size());
    }

    /**
     * 전체 텍스트 검색을 수행합니다
     */
    private Page<StatementDocument> performFullTextSearch(String keyword, Pageable pageable) {
        try {
            Page<StatementDocument> results = statementMongoRepository.fullTextSearch(keyword, pageable);

            if (results.isEmpty()) {
                log.debug("전체 텍스트 검색 결과 없음, 정규식 검색으로 전환: {}", keyword);
                results = statementMongoRepository.searchByRegex(keyword, pageable);
            }
            return results;
        } catch (Exception e) {
            log.warn("전체 텍스트 검색 실패, 정규식 검색으로 대체: keyword={}, error={}", keyword, e.getMessage());
            return statementMongoRepository.searchByRegex(keyword, pageable);
        }
    }

    /**
     * JPA entity -> MongoDB Document로 변환
     * @param entity
     * @return
     */
    private StatementDocument convertJpaEntityToDocument(Statement entity) {
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

}
