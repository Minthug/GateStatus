package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.service.response.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.timeline.TimelineEventDocument;
import com.example.GateStatus.domain.timeline.TimelineEventType;
import com.example.GateStatus.domain.timeline.repository.TimelineEventRepository;
import com.example.GateStatus.global.config.batch.BatchProcessResult;
import com.example.GateStatus.global.config.batch.BatchResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final TimelineEventRepository timelineRepository;
    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementRepository;
    private final ProposedBillService billService;

    private static final String SOURCE_TYPE_STATEMENT = "STATEMENT";
    private static final String SOURCE_TYPE_BILL = "BILL";
    private static final String SOURCE_TYPE_CUSTOM = "CUSTOM";
    private static final int DEFAULT_BATCH_SIZE = 50;





// ============================================
// 향후 확장용 고급 배치 처리 (Advanced Version)
// 대용량 처리 필요시 활성화 예정
// ============================================
        /**
         * 발언 데이터를 배치로 타임라인에 동기화
         * 대량의 발언 데이터를 효율적으로 처리하기 위해 배치 단위로 분할하여 처리
         * @param startDate
         * @param endDate
         * @param batchSize
         * @return
         */
        @Transactional
        public BatchProcessResult syncStatementsToTimelineBatch(LocalDate startDate, LocalDate endDate, int batchSize) {
            log.info("배치 발언 동기화 시작: {} ~ {}, batchSize={}", startDate, endDate, batchSize);

            List<StatementDocument> allStatements = statementRepository.findByPeriod(startDate, endDate);

            List<StatementDocument> newStatements = allStatements.stream()
                    .filter(statement -> !timelineRepository.existsBySourceTypeAndSourceId(
                            SOURCE_TYPE_STATEMENT, statement.getId()))
                    .collect(Collectors.toList());

            int totalCount = newStatements.size();
            int processCount = 0;
            int errorCount = 0;
            List<String> errorIds = new ArrayList<>();

            for (int i = 0; i < totalCount; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalCount);
                List<StatementDocument> batch = newStatements.subList(i, endIndex);

                try {
                    BatchResult batchResult = processBatchStatements(batch);
                    processCount += batchResult.successCount();
                    errorCount += batchResult.errorCount();
                    errorIds.addAll(batchResult.errorIds());

                    log.debug("배치 처리 완료: {}/{}, 성공={}, 실패={}",
                            endIndex, totalCount, batchResult.successCount(), batchResult.errorCount());

                } catch (Exception e) {
                    log.error("배치 처리 중 전체 실패: batch {}-{}, error={}", i, endIndex, e.getMessage());
                    errorCount += batch.size();
                    batch.forEach(stmt -> errorIds.add(stmt.getId()));
                }
            }

            BatchProcessResult result = new BatchProcessResult(
                    totalCount, processCount, errorCount, errorIds,
                    LocalDateTime.now().minusSeconds(1), LocalDateTime.now()
            );

            log.info("배치 발언 동기화 완료: 전체={}, 성공={}, 실패={}", totalCount, processCount, errorCount);
            return result;
        }

        /**
         * 개별 배치를 별도 트랜잭션으로 처리
         * 하나의 배치에서 오류가 발생해도 다른 배치에 영향을 주지 않는다.
         *
         * @return
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public BatchResult processBatchStatements(List<StatementDocument> statements) {
            List<TimelineEventDocument> events = new ArrayList<>();
            List<String> errorIds = new ArrayList<>();
            int successCount = 0;

            for (StatementDocument statement : statements) {
                try {
                    TimelineEventDocument event = createStatementEvent(statement, statement.getId());
                    events.add(event);
                    successCount++;
                } catch (Exception e) {
                    log.warn("발언 처리 실패: statementId={}, error={}", statement.getId(), e.getMessage());
                    errorIds.add(statement.getId());
                }
            }

            if (!events.isEmpty()) {
                timelineRepository.saveAll(events);
            }

            return new BatchResult(successCount, errorIds.size(), errorIds);
        }

        /**
         * 개선된 스케쥴러 - 배치 처리 적용
         */
//        @Scheduled(cron = "0 0 0 * * ?")
        @Transactional
        public void syncStatementsToTimelineScheduled() {
            LocalDate startDate = LocalDate.now().minusWeeks(1);
            LocalDate endDate = LocalDate.now();

            BatchProcessResult result = syncStatementsToTimelineBatch(startDate, endDate, 50);

            if (!result.errorIds().isEmpty()) {
                log.warn("실패한 발언들 재처리 시도: count={}", result.errorIds().size());
                retryFailedStatements(result.errorIds());
            }
        }

        /**
         * 실패한 발언들을 개별적으로 재처리
         * @param failedIds
         */
        @Async
        public void retryFailedStatements(List<String> failedIds) {
            for (String statementId : failedIds) {
                try {
                    addStatementToTimeline(statementId);
                    log.info("재처리 성공: statementId={}", statementId);
                } catch (Exception e) {
                    log.error("재처리 실패: statementId={}, error={}", statementId, e.getMessage());
                }
            }
        }

    /**
     * 특정 정치인의 전체 타임라인을 최신순으로 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> getFigureTimeline(Long figureId, Pageable pageable) {
        validateFigureExists(figureId);

        return timelineRepository.findByFigureIdOrderByEventDateDesc(figureId, pageable)
                .map(TimelineEventResponse::from);
    }


// ============================================
// 향후 확장용 고급 배치 처리 (Advanced Version)
// 대용량 처리 필요시 활성화 예정
// ============================================

    /**
     * 특정 정치인의 타임라인 조회 (타입 필터링)
     * @param figureId
     * @param type
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> getFigureTimelineByType(Long figureId, TimelineEventType type, Pageable pageable) {
        validateFigureExists(figureId);

        return timelineRepository.findByFigureIdAndEventTypeOrderByEventDateDesc(figureId, type, pageable)
                .map(TimelineEventResponse::from);
    }

    /**
     * 특정 기간 내 정치인의 타임라인 조회
     * @param figureId
     * @param startDate
     * @param endDate
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> getFigureTimelineByDateRange(Long figureId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        validateFigureExists(figureId);
        validateDateRange(startDate, endDate);

        return timelineRepository.findByFigureIdAndDateRange(figureId, startDate, endDate, pageable)
                .map(TimelineEventResponse::from);
    }


    /**
     * 특정 정치인의 타임라인에서 키워드를 포함하는 이벤트를 검색합니다.
     * 제목과 설명 필드에서 대소문자 구분없이 검색됩니다.
     *
     * @param figureId
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> searchFigureTimeline(Long figureId, String keyword, Pageable pageable) {
        validateFigureExists(figureId);
        validateKeyword(keyword);

        return timelineRepository.searchByKeyword(figureId, keyword, pageable)
                .map(TimelineEventResponse::from);
    }



    /**
     * 발언 데이터를 타임라인 이벤트로 추가
     * 이미 등록된 발언의 경우 기존 이벤트를 반환
     * @param statementId
     * @return
     */
    @Transactional
    public TimelineEventResponse addStatementToTimeline(String statementId) {
        StatementDocument statement = getStatementOrThrow(statementId);

        Optional<TimelineEventDocument> existingEvent = findExistingEvent(SOURCE_TYPE_STATEMENT, statementId);
        if (existingEvent.isPresent()) {
            log.debug("이미 타임라인에 등록된 발언입니다: {}", statementId);
            return TimelineEventResponse.from(existingEvent.get());
        }

        TimelineEventDocument timelineEvent = createStatementEvent(statement, statementId);
        TimelineEventDocument savedEvent = timelineRepository.save(timelineEvent);

        log.info("발언이 타임라인에 추가되었습니다: statementId={}, eventId={}", statementId, savedEvent.getId());
        return TimelineEventResponse.from(savedEvent);
    }

    /**
     * 법안 데이터를 타임라인 이벤트로 추가
     * 이미 등록된 법안의 경우 기존 이벤트를 반환
     * @param billId
     * @param figureId
     * @return
     */
    @Transactional
    public TimelineEventResponse addBillToTimeline(String billId, Long figureId) {
        ProposedBillResponse billResponse = billService.findBillById(billId);
        Figure figure = getFigureOrThrow(figureId);

        Optional<TimelineEventDocument> existingEvent = findExistingEvent(SOURCE_TYPE_BILL, billId);
        if (existingEvent.isPresent()) {
            log.debug("이미 타임라인에 등록된 법안입니다: {}", billId);
            return TimelineEventResponse.from(existingEvent.get());
        }

        TimelineEventDocument timelineEvent = createBillEvent(billResponse, figure, billId);
        TimelineEventDocument savedEvent = timelineRepository.save(timelineEvent);

        log.info("법안이 타임라인에 추가되었습니다: billId={}, figureId={}, eventId={}", billId, figureId, savedEvent.getId());
        return TimelineEventResponse.from(savedEvent);
    }

    /**
     * 사용자 정의 타임라인 이벤트를 추가
     * 임명, 수상, 논란 등 자동으로 수집되지 않는 이벤트를 수동으로 추가할 때 사용
     *
     * @param figureId
     * @param title
     * @param description
     * @param eventDate
     * @param type
     * @return
     */
    @Transactional
    public TimelineEventResponse addCustomEvent(Long figureId, String title, String description,
                                                LocalDate eventDate, TimelineEventType type) {
        Figure figure = getFigureOrThrow(figureId);
        validateCustomEventParameters(title, description, eventDate, type);

        TimelineEventDocument event = createCustomEvent(figure, title, description, eventDate, type);
        TimelineEventDocument savedEvent = timelineRepository.save(event);

        log.info("커스텀 이벤트가 타임라인에 추가되었습니다: figureId={}, type={}, eventId={}", figureId, type, savedEvent.getId());
        return TimelineEventResponse.from(savedEvent);
    }

//    /**
//     * 매일 자정에 실행되어 최근 1주일간의 발언 데이터를 자동으로 타임라인에 동기화합니다.
//     * 이미 등록된 발언은 건너뛰고 새로운 발언만 추가됩니다.
//     *
//     * 매일 자정 실행
//     */
//    @Scheduled(cron = "0 0 0 * * ?")
//    @Transactional
//    public void syncStatementsToTimeline() {
//        log.info("발언 데이터 타임라인 동기화 시작");
//
//        LocalDate startDate = LocalDate.now().minusWeeks(1);
//        List<StatementDocument> statements = statementRepository.findByPeriod(startDate, LocalDate.now());
//
//        int processedCount = 0;
//        int skippedCount = 0;
//
//        for (StatementDocument statement : statements) {
//            if (timelineRepository.existsBySourceTypeAndSourceId(SOURCE_TYPE_STATEMENT, statement.getId())) {
//                skippedCount++;
//                continue;
//            }
//
//            try {
//                addStatementToTimeline(statement.getId());
//                processedCount++;
//            } catch (Exception e) {
//                log.error("발언 동기화 중 오류 발생: statementId={}, error={}", statement.getId(), e.getMessage());
//            }
//        }
//        log.info("발언 데이터 타임라인 동기화 완료: 처리={}, 스킵={}, 전체={}", processedCount, skippedCount, statements.size());
//    }

    /**
     * 타임라인 이벤트 삭제
     * 물리적 삭제를 수행하며, 연관된 원본 데이터(발언, 법안 등)는 삭제되지 않는다.
     * @param eventId
     */
    @Transactional
    public void deleteTimelineEvent(String eventId) {
        TimelineEventDocument event = timelineRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 타임라인 이벤트가 존재하지 않습니다 " + eventId));

        timelineRepository.delete(event);
        log.info("타임라인 이벤트 삭제: {}", eventId);
    }

    // ============================================
// 현재 사용중인 배치 처리 (Simple Version)
// ============================================

    /**
     * 🎯 핵심 개선: 배치 처리로 발언 데이터 동기화
     * 매일 자정 실행 - 메모리 효율적 처리
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void syncStatementsToTimeline() {
        log.info("발언 데이터 타임라인 동기화 시작");

        LocalDate startDate = LocalDate.now().minusWeeks(1);
        LocalDate endDate = LocalDate.now();

        // 🔥 핵심 개선: 배치 처리
        int totalProcessed = syncStatementsInBatches(startDate, endDate);

        log.info("발언 데이터 타임라인 동기화 완료: {} 건 처리", totalProcessed);
    }

    /**
     * 🚀 배치 처리 핵심 메서드
     * - 50개씩 나눠서 처리하여 메모리 효율성 확보
     * - 개별 실패가 전체에 영향주지 않도록 격리
     * - 진행상황 실시간 추적 가능
     */
    private int syncStatementsInBatches(LocalDate startDate, LocalDate endDate) {
        int processedCount = 0;
        int batchNumber = 0;

        while (true) {
            // 페이징으로 배치별 조회 (메모리 효율적)
            Pageable pageable = PageRequest.of(batchNumber, DEFAULT_BATCH_SIZE);
            List<StatementDocument> batch = statementRepository
                    .findByStatementDateBetween(startDate, endDate, pageable);

            if (batch.isEmpty()) {
                break; // 더 이상 처리할 데이터 없음
            }

            log.info("배치 {} 처리 시작: {} 건", batchNumber + 1, batch.size());

            // 배치 내에서 개별 처리
            int batchProcessed = 0;
            for (StatementDocument statement : batch) {
                try {
                    if (!timelineRepository.existsBySourceTypeAndSourceId(SOURCE_TYPE_STATEMENT, statement.getId())) {
                        addStatementToTimeline(statement.getId());
                        batchProcessed++;
                        processedCount++;
                    }
                } catch (Exception e) {
                    // 개별 실패는 로그만 남기고 계속 진행
                    log.error("발언 동기화 실패: statementId={}, error={}",
                            statement.getId(), e.getMessage());
                }
            }

            batchNumber++;
            log.info("배치 {} 처리 완료: {}/{} 건 성공", batchNumber, batchProcessed, batch.size());

            // DB 부하 방지를 위한 잠시 대기 (마지막 배치가 아닌 경우)
            if (batch.size() == DEFAULT_BATCH_SIZE) {
                try {
                    Thread.sleep(100); // 0.1초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("배치 처리 중단됨");
                    break;
                }
            }
        }

        return processedCount;
    }



    // === Private Helper Methods ===
    /**
     * 정치인 존재 여부를 확인합니다.
     * @param figureId
     */
    private void validateFigureExists(Long figureId) {
        if (!figureRepository.existsById(figureId)) {
            throw new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + figureId);
        }
    }

    /**
     * 날짜 범위의 유효성을 검증합니다.
     * @param startDate
     * @param endDate
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작날짜는 종료날짜보다 늦을 수 없습니다: start=" + startDate + ", end=" + endDate);
        }
    }

    /**
     * 검색 키워드의 유효성을 검증
     * @param keyword
     */
    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 비어있을 수 있습니다");
        }
    }

    /**
     * 정치인 엔티티를 조회하거나 예외를 발생시킴
     * @param figureId
     * @return
     */
    private Figure getFigureOrThrow(Long figureId) {
        return figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + figureId));
    }

    /**
     * 발언 문서를 조회하거나 예외를 발생
     * @param statementId
     * @return
     */
    private StatementDocument getStatementOrThrow(String statementId) {
        return statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다: " + statementId));
    }

    /**
     * 기존 타임라인 이벤트를 조회
     * @return
     */
    private Optional<TimelineEventDocument> findExistingEvent(String sourceType, String statementId) {
        List<TimelineEventDocument> events = timelineRepository.findBySourceTypeAndSourceId(sourceType, statementId);
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    /**
     * 발언 기반 타임라인 이벤트를 생성
     * @param statement
     * @param statementId
     * @return
     */
    private TimelineEventDocument createStatementEvent(StatementDocument statement, String statementId) {
        return TimelineEventDocument.builder()
                .figureId(statement.getFigureId())
                .figureName(statement.getFigureName())
                .eventDate(statement.getStatementDate())
                .title(statement.getTitle())
                .description(statement.getContent())
                .eventType(TimelineEventType.STATEMENT)
                .sourceType(SOURCE_TYPE_STATEMENT)
                .sourceId(statementId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 법안 기반 타임라인 이벤트를 생성
     * @param billResponse
     * @param figure
     * @param billId
     * @return
     */
    private TimelineEventDocument createBillEvent(ProposedBillResponse billResponse, Figure figure, String billId) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("billStatus", billResponse.billStatus().toString());
        additionalData.put("proposerCount", billResponse.coProposers().size() + 1);

        return TimelineEventDocument.builder()
                .figureId(figure.getId())
                .figureName(figure.getName())
                .eventDate(billResponse.processDate())
                .title(billResponse.billName())
                .description("법안 발의: " + billResponse.billName())
                .eventType(TimelineEventType.BILL_PROPOSED)
                .sourceType(SOURCE_TYPE_BILL)
                .sourceId(billId)
                .additionalData(additionalData)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 커스텀 타임라인 이벤트를 생성
     * @param figure
     * @param description
     * @param eventDate
     * @param type
     * @return
     */
    private TimelineEventDocument createCustomEvent(Figure figure, String title, String description,
                                                    LocalDate eventDate, TimelineEventType type) {
        return TimelineEventDocument.builder()
                .figureId(figure.getId())
                .figureName(figure.getName())
                .eventDate(eventDate)
                .title(title)
                .description(description)
                .eventType(type)
                .sourceType(SOURCE_TYPE_CUSTOM)
                .sourceId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 커스텀 이벤트 파라미터의 유효성을 검증
     * @param title
     * @param description
     * @param eventDate
     * @param type
     */
    private void validateCustomEventParameters(String title, String description, LocalDate eventDate, TimelineEventType type) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("이벤트 제목은 비어있을 수 없습니다");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("이벤트 설명은 비어있을 수 없습니다");
        }

        if (eventDate == null) {
            throw new IllegalArgumentException("이벤트 날짜는 필수입니다");
        }

        if (type == null) {
            throw new IllegalArgumentException("이벤트 타입은 필수입니다");
        }
    }
}
