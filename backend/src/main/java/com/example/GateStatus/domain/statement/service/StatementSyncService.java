package com.example.GateStatus.domain.statement.service;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.statement.entity.StatementType;
import com.example.GateStatus.domain.statement.exception.StatementSyncException;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.statement.service.response.StatementApiDTO;
import com.example.GateStatus.global.config.open.AssemblyApiResponse;
import com.example.GateStatus.global.openAi.OpenAiClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementSyncService {

    private final WebClient.Builder webclientBuilder;
    private final StatementMongoRepository statementRepository;
    private final FigureRepository figureRepository;
    private final StatementApiMapper statementApiMapper;
    private final StatementService statementService;
    private final FigureApiService figureApiService;
    private final StatementApiService apiService;
    private final OpenAiClient openAiClient;

    // ========== 비동기 작업 상태 관리 ==========
    private final ConcurrentMap<String, SyncJobStatus> jobStatusMap = new ConcurrentHashMap<>();


    /**
     * 국회방송국 API에서 특정 인물의 뉴스/발언 정보를 가져와 저장
     * @param figureName
     * @return
     */
    @Transactional
    public int syncStatementsByFigure(String figureName) {
        log.info("국회방송국 API에서 '{}' 인물 발언 정보 동기화 시작", figureName);

        try {
            Figure figure = getOrCreateFigure(figureName);

            AssemblyApiResponse<String> apiResponse = apiService.fetchStatementsByFigure(figureName);
            if (!apiResponse.isSuccess()) {
                log.error("API 호출 실패: {}", apiResponse.resultMessage());
                throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
            }

            List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
            if (apiDtos.isEmpty()) {
                log.info("'{}' 인물의 새로운 발언 정보가 없습니다", figureName);
                return 0;
            }

            int syncCount = saveNewStatements(apiDtos, figure);
            log.info("'{}' 인물 발언 정보 동기화 완료. 총 {} 건 처리됨", figureName, syncCount);
            return syncCount;
        } catch (Exception e) {
            log.error("발언 정보 동기화 실패: figureName={}, error={}", figureName, e.getMessage(), e);
            throw new StatementSyncException("발언 정보 동기화 실: " + e.getMessage(), e);
        }
    }


    /**
     * 특정 기간 동안의 모든 발언 정보를 동기화
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional
    public int syncStatementsByPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("국회방송국 API에서 기간({} ~ {}) 발언 정보 동기화 시작", startDate, endDate);

        try {
            AssemblyApiResponse<String> apiResponse = apiService.fetchStatementsByPeriod(startDate, endDate);
            if (!apiResponse.isSuccess()) {
                throw new RuntimeException("API 호출 실패: " + apiResponse.resultMessage());
            }

            List<StatementApiDTO> apiDtos = statementApiMapper.map(apiResponse);
            int syncCount = 0;

            for (StatementApiDTO dto : apiDtos) {
                if (statementRepository.existsByOriginalUrl(dto.originalUrl())) {
                    log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
                    continue;
                }

                Figure figure = getOrCreateFigure(dto.figureName());
                StatementDocument document = convertApiDtoToDocument(dto, figure);
                statementRepository.save(document);
                syncCount++;
            }

            log.info("기간({} ~ {}) 발언 정보 동기화 완료. 총 {} 건 처리됨", startDate, endDate, syncCount);
            return syncCount;
        } catch (Exception e) {
            log.error("기간별 발언 정보 동기화 실패: {} ~ {}, error={}", startDate, endDate, e.getMessage(), e);
            throw new StatementSyncException("기간별 발언 정보 동기화 실패", e);
        }
    }

    @Async("statementSyncExecutor")
    @Transactional
    public CompletableFuture<Integer> syncStatementsByPeriodAsync(LocalDate startDate, LocalDate endDate) {
        log.info("기간별 발언 정보 비동기 동기화 시작: {} ~ {}", startDate, endDate);

        try {
            AssemblyApiResponse<String> apiResponse = apiService.fetchStatementsByPeriod(startDate, endDate);
            if (!apiResponse.isSuccess()) {
                throw new StatementSyncException("기간별 API 호출 실패: " + apiResponse.resultMessage());
            }

            List<StatementApiDTO> apiDTOS = statementApiMapper.map(apiResponse);
            int syncCount = 0;

            for (StatementApiDTO dto : apiDTOS) {
                if (statementRepository.existsByOriginalUrl(dto.originalUrl())) {
                    continue;
                }

                Figure figure = getOrCreateFigure(dto.figureName());
                StatementDocument document = convertApiDtoToDocument(dto, figure);
                statementRepository.save(document);
                syncCount++;
            }

            log.info("기간별 발언 정보 비동기 동기화 완료: {} ~ {}, 총 {}건", startDate, endDate, syncCount);
            return CompletableFuture.completedFuture(syncCount);
        } catch (Exception e) {
            log.error("기간별 발언 정보 비동기 동기화 실패: {} ~ {}, error={}", startDate, endDate, e.getMessage(), e);
            throw new StatementSyncException("기간별 발언 정보 동기화 실패", e);
        }
    }


    @Transactional
    public int syncAllStatements() {
        log.info("모든 국회의원의 발언 정보 동기화 시작");
        List<Figure> allFigures = figureRepository.findByFigureType(FigureType.POLITICIAN);

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        log.info("동기화 대상 국회의원: {}명", allFigures.size());

        int totalSuccess = 0;
        int totalFail = 0;

        for (Figure figure : allFigures) {
            try {
                String name = figure.getName();
                int success = syncStatementsByFigure(name);
                totalSuccess += success;
                log.info("국회의원 {}의 발언 정보 동기화 완료: {}건", name, success);
            } catch (Exception e) {
                totalFail++;
                log.error("국회의원 {}의 발언 정보 동기화 중 오류: {}", figure.getName(), e.getMessage(), e);
            }
        }
        log.info("모든 국회의원 발언 정보 동기화 완료: 총 {}명 중 성공 {}건, 실패 {}건",
                allFigures.size(), totalSuccess, totalFail);
        return totalSuccess;
    }

    /**
     * 비동기로 발언 정보 동기화를 시작합니다.
     * @return
     */
    public String syncStatementsAsync() {
        String jobId = UUID.randomUUID().toString();

        SyncJobStatus jobStatus = new SyncJobStatus(jobId);
        jobStatusMap.put(jobId, jobStatus);

        CompletableFuture.runAsync(() -> {
            processStatementSyncJob(jobId);
        });

        return jobId;
    }

    /**
     * 비동기로 특정 국회의원의 발언 정보 동기화를 시작합니다.
     * @return
     */
    public String syncStatementsByFigureAsync(String figureName) {
        String jobId = UUID.randomUUID().toString();

        SyncJobStatus jobStatus = new SyncJobStatus(jobId);
        jobStatus.setTotalTasks(1);
        jobStatusMap.put(jobId, jobStatus);

        CompletableFuture.runAsync(() -> {
            try {
                int count = syncStatementsByFigure(figureName);
                jobStatus.setSuccessCount(count);
                jobStatus.incrementCompletedTasks();
                jobStatus.setCompleted(true);
                jobStatus.setEndTime(LocalDateTime.now());
            } catch (Exception e) {
                jobStatus.setError(true);
                jobStatus.setErrorMessage(e.getMessage());
                jobStatus.incrementCompletedTasks();
                jobStatus.setCompleted(true);
                jobStatus.setEndTime(LocalDateTime.now());
            }
        });

        return jobId;
    }

    protected void processStatementSyncJob(String jobId) {
        log.info("발언 정보 동기화 작업({}) 시작", jobId);

        SyncJobStatus jobStatus = jobStatusMap.get(jobId);

        try {
            List<Figure> allFigures = figureRepository.findByFigureType(FigureType.POLITICIAN);

            if (allFigures.isEmpty()) {
                log.warn("동기화할 국회의원 정보가 없습니다");
                jobStatus.setTotalTasks(0);
                jobStatus.setCompleted(true);
                jobStatus.setEndTime(LocalDateTime.now());
                return;
            }

            // 작업 상태 업데이트
            jobStatus.setTotalTasks(allFigures.size());
            log.info("동기화 대상 국회의원: {}명", allFigures.size());

            int totalSuccess = 0;
            int totalFail = 0;

            for (Figure figure : allFigures) {
                try {
                    String name = figure.getName();
                    log.info("국회의원 {}의 발언 정보 동기화 시작", name);

                    int success = syncStatementsByFigure(name);
                    totalSuccess += success;
                    jobStatus.incrementSuccessCount();

                    log.info("국회의원 {}의 발언 정보 동기화 완료: {}건", name, success);
                } catch (Exception e) {
                    totalFail++;
                    jobStatus.incrementFailCount();
                    log.error("국회의원 {}의 발언 정보 동기화 중 오류: {}", figure.getName(), e.getMessage(), e);
                } finally {
                    jobStatus.incrementCompletedTasks();
                }
            }

            jobStatus.setCompleted(true);
            jobStatus.setEndTime(LocalDateTime.now());

            log.info("발언 정보 동기화 작업({}) 완료: 총 {}명 중 {}건 성공, {}건 실패",
                    jobId, allFigures.size(), totalSuccess, totalFail);
        } catch (Exception e) {
            log.error("발언 정보 동기화 작업({}) 중 오류 발생: {}", jobId, e.getMessage(), e);
            jobStatus.setError(true);
            jobStatus.setErrorMessage(e.getMessage());
            jobStatus.setCompleted(true);
            jobStatus.setEndTime(LocalDateTime.now());
        }
    }

    public SyncJobStatus getSyncJobStatus(String jobId) {
        return jobStatusMap.get(jobId);
    }


    private int saveNewStatements(List<StatementApiDTO> apiDtos, Figure figure) {
        int syncCount = 0;

        for (StatementApiDTO dto : apiDtos) {
            try {
                if (statementRepository.existsByOriginalUrl(dto.originalUrl())) {
                    log.debug("이미 존재하는 발언 건너뜀: {}", dto.originalUrl());
                    continue;
                }

                StatementDocument document = convertApiDtoToDocument(dto, figure);
                statementRepository.save(document);
                syncCount++;

                log.debug("새 발언 저장 완료: title={}, url={}", dto.title(), dto.originalUrl());
            } catch (Exception e) {
                log.warn("발언 저장 실패: title={}, error={}", dto.title(), e.getMessage());
            }
        }
        log.info("새 발언 저장 완료: 대상={}건, 저장={}건, 정치인={}", apiDtos.size(), syncCount, figure.getName());
        return syncCount;
    }

    private Figure getOrCreateFigure(String figureName) {
        Figure figure = figureRepository.findByName(figureName).orElse(null);

        if (figure == null) {
            log.info("DB에 국회의원 정보가 없어 API에서 동기화 시도: {}", figureName);
            try {
                figure = figureApiService.syncFigureInfoByName(figureName);
            } catch (Exception e) {
                log.error("국회의원 정보 동기화 실패: {} - {}", figureName, e.getMessage());
                throw new EntityNotFoundException("해당 인물이 존재하지 않습니다: " + figureName);
            }
        }
        return figure;
    }

    /**
     * API DTO -> MongoDB Document
     * @param dto
     * @param figure
     * @return
     */
    private StatementDocument convertApiDtoToDocument(StatementApiDTO dto, Figure figure) {
        return statementService.convertApiDtoToDocument(dto, figure);
    }


    private void enrichWithAiAnalysis(StatementDocument.StatementDocumentBuilder builder, String content) {
        try {
            // 키워드 추출
            List<String> keywords = openAiClient.extractKeywords(content);
            if (!keywords.isEmpty()) {
                builder.topics(keywords);
            }

            // 발언 유형 분류
            StatementType aiDeterminedType = openAiClient.classifyStatement(content);
            if (aiDeterminedType != StatementType.OTHER) {
                builder.type(aiDeterminedType);
            }

            // 요약 생성 (긴 내용인 경우)
            if (content.length() > 200) {
                String summary = openAiClient.summarizeStatement(content);
                if (summary != null && !summary.isEmpty()) {
                    builder.summary(summary);
                }
            }

            // 감성 분석
            Map<String, Double> sentiment = openAiClient.analyzeSentiment(content);
            if (!sentiment.isEmpty()) {
                Map<String, Object> nlpData = new HashMap<>();
                nlpData.put("sentiment", sentiment);
                builder.nlpData(nlpData);
            }

        } catch (Exception e) {
            log.warn("AI 분석 중 오류 발생: {}", e.getMessage());
            // AI 분석 실패는 전체 저장을 막지 않음
        }
    }

    /**
     * API 유형 코드를 StatementType으로 변환합니다
     * @param typeCode API 유형 코드
     * @return StatementType
     */
    private StatementType determineStatementType(String typeCode) {
        if (typeCode == null) {
            return StatementType.OTHER;
        }

        return switch (typeCode) {
            case "SPEECH" -> StatementType.SPEECH;
            case "INTERVIEW" -> StatementType.INTERVIEW;
            case "PRESS" -> StatementType.PRESS_RELEASE;
            case "DEBATE" -> StatementType.DEBATE;
            case "ASSEMBLY" -> StatementType.ASSEMBLY_SPEECH;
            case "COMMITTEE" -> StatementType.COMMITTEE_SPEECH;
            case "MEDIA" -> StatementType.MEDIA_COMMENT;
            case "SNS" -> StatementType.SOCIAL_MEDIA;
            default -> StatementType.OTHER;
        };
    }
}
