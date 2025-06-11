package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.proposedBill.service.StatementValidator;
import com.example.GateStatus.domain.statement.entity.Statement;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.exception.StatementNotFoundException;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.statement.service.request.FactCheckRequest;
import com.example.GateStatus.domain.statement.service.request.StatementRequest;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.domain.statement.service.response.StatementResponse;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.openAi.OpenAiClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementMongoRepository;
    private final StatementApiMapper mapper;
    private final WebClient.Builder webclientBuilder;
    private final OpenAiClient openAiClient;
    private final FigureApiService figureApiService;
    private final StatementValidator validator;


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
    @Transactional
    public List<StatementResponse> findPopularStatements(int limit) {
        return statementMongoRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, limit))
                .stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 발언 내용만으로 검색 (페이징 없음)
     * @param keyword
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> searchStatementContent(String keyword) {
        log.info("발언 내용 검색: 키워드 = {}", keyword);
        List<StatementDocument> statements = statementMongoRepository.findByContentContainingKeyword(keyword);
        log.info("발언 내용 검색 완료: 키워드 = {}, 결과 수 = {}", keyword, statements.size());
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 정확한 문구로 발언 검색
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> searchExtractPhrase(String phrase) {
        log.info("정확한 문구로 발언 검색: 문구 = {}", phrase);
        String escapedPhrase = Pattern.quote(phrase);
        List<StatementDocument> statements = statementMongoRepository.findByExactPhraseInContent(escapedPhrase);
        log.info("정확한 문구 검색 완료: 문구 = {}, 결과 수 = {}", phrase, statements.size());
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 여러 키워드를 모두 포함하는 발언 검색 (AND 조건)
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> searchWithMultipleKeywords(List<String> keywords) {
        if (keywords == null || keywords.size() < 2) {
            throw new IllegalArgumentException("최소 2개 이상의 키워드가 필요합니다");
        }

        log.info("다중 키워드 발언 검색: 키워드 = {}", keywords);

        List<StatementDocument> results = statementMongoRepository.findByMultipleKeywords(keywords.get(0), keywords.get(1));

        if (keywords.size() > 2) {
            for (int i = 0; i < keywords.size(); i++) {
                final String keyword = keywords.get(i);
                results = results.stream()
                        .filter(statement -> statement.getContent().toLowerCase()
                                .contains(keyword.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        log.info("다중 키워드 검색 완료: 키워드 = {}, 결과 수 = {}", keywords, results.size());
        return results.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 키워드로 발언 검색 (페이징 적용, 제목과 내용 모두 검색 )
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> searchStatements(String keyword, Pageable pageable) {
        log.info("발언 검색 요청 시작: 키워드 = {}", keyword);

        try {
            Page<StatementDocument> results = statementMongoRepository.fullTextSearch(keyword, pageable);

            if (results.isEmpty()) {
                log.info("텍스트 검색 결과 없음, 정규식 검색으로 전환: {}", keyword);
                results = statementMongoRepository.searchByRegex(keyword, pageable);
            }

            // 검색 결과 로그
            log.info("발언 검색 완료: 키워드 = {}, 결과 수 = {}", keyword, results.getTotalElements());


            return results.map(StatementResponse::from);
        } catch (Exception e) {
            log.error("발언 검색 중 오류 발생: {}", e.getMessage(), e);
            Page<StatementDocument> fallbackResults = statementMongoRepository.searchByRegex(keyword, pageable);
            return fallbackResults.map(StatementResponse::from);
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
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 발언 중 특정 키워드를 포함하는 발언 검색
     * @param keyword
     * @param limit
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> searchRecentStatements(String keyword, int limit) {
        log.info("최근 발언 검색: 키워드 = {}, 제한 = {}", keyword, limit);
        List<StatementDocument> statements = statementMongoRepository.findByKeywordOrderByStatementDateDesc(
                keyword, PageRequest.of(0, limit));
        log.info("최근 발언 검색 완료: 키워드 = {}, 결과 수 = {}", keyword, statements.size());
        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 발언 길이에 따른 검색 (긴 발언, 짧은 발언 구분)
     * @param minLength
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findLongStatements(int minLength, Pageable pageable) {
        log.info("긴 발언 검색: 최소 길이 = {}", minLength);
        List<StatementDocument> statements = statementMongoRepository.findByContentLengthGreaterThan(minLength, pageable);
        log.info("긴 발언 검색 완료: 최소 길이 = {}, 결과 수 = {}", minLength, statements.size());

        return statements.stream()
                .map(StatementResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 발언 길이에 따른 검색 (짧은 발언)
     * @param maxLength
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public List<StatementResponse> findShortStatements(int maxLength, Pageable pageable) {
        log.info("짧은 발언 검색: 최대 길이 = {}", maxLength);
        List<StatementDocument> statements = statementMongoRepository.findByContentLengthLessThan(maxLength, pageable);
        log.info("짧은 발언 검색 완료: 최대 길이 = {}, 결과 수 = {}", maxLength, statements.size());

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
        return statementMongoRepository.findByPeriod(startDate, endDate)
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
        return statementMongoRepository.findByFactCheckScoreGreaterThanEqual(minScore)
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
        return statementMongoRepository.findBySource(source)
                .stream()
                .map(StatementResponse::from)
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


    @Transactional
    public int syncStatementsByFigure(String figureName) {
        log.info("국회방송국 API에서 '{}' 인물 발언 정보 동기화 시작", figureName);

        // DB에서 국회의원 조회
        Figure figure = figureRepository.findByName(figureName)
                .orElse(null);

        // DB에 없으면 API에서 가져와 저장
        if (figure == null) {
            log.info("DB에 국회의원 정보가 없어 API에서 동기화 시도: {}", figureName);
            try {
                figure = figureApiService.syncFigureInfoByName(figureName);
            } catch (Exception e) {
                log.error("국회의원 정보 동기화 실패: {} - {}", figureName, e.getMessage());
                throw new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName);
            }
        }


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

            StatementDocument document = convertApiDtoToDocument(dto, figure);
            statementMongoRepository.save(document);
            syncCount++;
        }

        log.info("'{}' 인물 발언 정보 동기화 완료. 총 {} 건 처리됨", figureName, syncCount);
        return syncCount;
    }

    public AssemblyApiResponse<String> fetchStatementsByFigure(String figureName) {
        WebClient webClient = webclientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();

        String xmlResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/nauvppbxargkmyovh")
                        .queryParam("apiKey", apikey)
                        .queryParam("name", figureName)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String resultCode = extractResultCode(xmlResponse);
        String resultMessage = extractResultMessage(xmlResponse);

        return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
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

    /**
     * API의 발언 유형 코드를 애플리케이션 StatementType으로 변환
     * @param typeCode
     * @return
     */
    public StatementType determineStatementType(String typeCode) {
        switch (typeCode) {
            case "SPEECH":
                return StatementType.SPEECH;
            case "INTERVIEW":
                return StatementType.INTERVIEW;
            case "PRESS":
                return StatementType.PRESS_RELEASE;
            case "DEBATE":
                return StatementType.DEBATE;
            case "ASSEMBLY":
                return StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE":
                return StatementType.COMMITTEE_SPEECH;
            case "MEDIA":
                return StatementType.MEDIA_COMMENT;
            case "SNS":
                return StatementType.SOCIAL_MEDIA;
            default:
                return StatementType.OTHER;
        }
    }

//    /**
//     * 본문에서 검증 가능한 항목 추출
//     */
//    private List<String> extractCheckableItems(String content) {
//        if (content == null || content.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        List<String> items = new ArrayList<>();
//
//        // 간단한 구현 예
//        String[] sentences = content.split("\\. ");
//        for (String sentence : sentences) {
//            if (sentence.matches(".*\\d+.*") ||
//                    sentence.contains("이다") ||
//                    sentence.contains("했다") ||
//                    sentence.contains("라고 말했") ||
//                    sentence.contains("주장")) {
//
//                items.add(sentence.trim() + (sentence.endsWith(".") ? "" : "."));
//            }
//        }
//
//        // 최대 3개 항목으로 제한
//        return items.stream().limit(3).collect(Collectors.toList());
//    }

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
                .type(determineStatementType(dto.typeCode()))
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

    @Transactional
    public StatementResponse performFactCheck(String id) {
        StatementDocument statement = statementMongoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다 " + id));

        try {
            log.info("발언 ID {} 팩트체크 시작", id);
             OpenAiClient.FactCheckResult result = openAiClient.factCheckStatement(statement.getContent());

             statement.updateFactCheck(result.getScore(), result.getExplanation());

             if (statement.getNlpData() == null) {
                 statement.setNlpData(new HashMap<>());
             }

             statement.getNlpData().put("checkableItems", result.getCheckableItems());

             statement.setUpdatedAt(LocalDateTime.now());

             StatementDocument savedStatement = statementMongoRepository.save(statement);
             log.info("발언 ID {} 팩트체크 완료: 점수={}", id, result.getScore());

             return StatementResponse.from(savedStatement);
        } catch (Exception e) {
            log.error("팩트체크 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("팩트체크 처리 중 오류 발생", e);
        }
    }

    @Transactional
    public StatementResponse updateFactCheck(String id, FactCheckRequest request) {
        StatementDocument statement = statementMongoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + id));

        // 기본 팩트체크 정보 업데이트
        statement.updateFactCheck(request.score(), request.result());

        // 추가 정보 NLP 데이터에 저장
        Map<String, Object> nlpData = statement.getNlpData();

        if (request.checkableItems() != null) {
            nlpData.put("checkableItems", request.checkableItems());
        }

        if (request.evidenceUrl() != null) {
            nlpData.put("evidenceUrl", request.evidenceUrl());
        }

        if (request.checkerName() != null) {
            nlpData.put("checkerName", request.checkerName());
        }

        if (request.checkerInstitution() != null) {
            nlpData.put("checkerInstitution", request.checkerInstitution());
        }

        if (request.analysisDetail() != null) {
            nlpData.put("analysisDetail", request.analysisDetail());
        }

        statement.setUpdatedAt(LocalDateTime.now());
        StatementDocument savedStatement = statementMongoRepository.save(statement);

        log.info("발언 ID {} 팩트체크 수동 업데이트 완료: 점수={}", id, request.score());
        return StatementResponse.from(savedStatement);
    }

    /**
     * API에서 특정 기간의 발언 정보 가져오기
     * @param startDate
     * @param endDate
     * @return
     */
    public AssemblyApiResponse<String> fetchStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        WebClient webClient = webclientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .build();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        String xmlResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news/period")
                        .queryParam("apiKey", apikey)
                        .queryParam("startDate", startDate.format(formatter))
                        .queryParam("endDate", endDate.format(formatter))
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String resultCode = extractResultCode(xmlResponse);
        String resultMessage = extractResultMessage(xmlResponse);

        return new AssemblyApiResponse<>(resultCode, resultMessage, xmlResponse);
    }

    /**
     * XML 응답에서 결과 메시지 추출
     * @param xmlResponse
     * @return
     */
    private String extractResultMessage(String xmlResponse) {
        if (xmlResponse.contains("<MESSAGE>")) {
            int start = xmlResponse.indexOf("<MESSAGE>") + "<MESSAGE>".length();
            int end = xmlResponse.indexOf("</MESSAGE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "처리 중 오류가 발생했습니다";
    }

    /**
     * XML 응답에서 결과 코드 추출
     * @param xmlResponse
     * @return
     */
    private String extractResultCode(String xmlResponse) {
        if (xmlResponse.contains("<CODE>")) {
            int start = xmlResponse.indexOf("<CODE>") + "<CODE>".length();
            int end = xmlResponse.indexOf("</CODE>");
            if (start > 0 && end > start) {
                return xmlResponse.substring(start, end);
            }
        }
        return "99";
    }

    public void migrateFromJpa(List<Statement> statements) {
        List<StatementDocument> documents = statements.stream()
                .map(this::convertJpaEntityToDocument)
                .collect(Collectors.toList());

        statementMongoRepository.saveAll(documents);
        log.info("{}개의 발언 데이터를 JPA에서 MongoDB로 마이그레이션 했습니다", documents.size());
    }

    // ========== Private Helper 메서드들 ==========
    private StatementDocument findStatementDocumentById(String id) {
        return statementMongoRepository.findById(id)
                .orElseThrow(() -> new StatementNotFoundException(id));
    }
}
