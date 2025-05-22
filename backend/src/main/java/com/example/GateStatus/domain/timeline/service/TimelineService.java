package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.timeline.TimelineEventDocument;
import com.example.GateStatus.domain.timeline.TimelineEventType;
import com.example.GateStatus.domain.timeline.repository.TimelineEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        TimelineEventDocument event = createCustomEvent(figure, description, eventDate, type);
        TimelineEventDocument savedEvent = timelineRepository.save(event);

        log.info("커스텀 이벤트가 타임라인에 추가되었습니다: figureId={}, type={}, eventId={}", figureId, type, savedEvent.getId());
        return TimelineEventResponse.from(savedEvent);
    }

    /**
     * 매일 자정에 실행되어 최근 1주일간의 발언 데이터를 자동으로 타임라인에 동기화합니다.
     * 이미 등록된 발언은 건너뛰고 새로운 발언만 추가됩니다.
     *
     * 매일 자정 실행
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void syncStatementsToTimeline() {
        log.info("발언 데이터 타임라인 동기화 시작");

        LocalDate startDate = LocalDate.now().minusWeeks(1);
        List<StatementDocument> statements = statementRepository.findByPeriod(startDate, LocalDate.now());

        int processedCount = 0;
        int skippedCount = 0;

        for (StatementDocument statement : statements) {
            if (timelineRepository.existsBySourceTypeAndSourceId(SOURCE_TYPE_STATEMENT, statement.getId())) {
                skippedCount++;
                continue;
            }

            try {
                addStatementToTimeline(statement.getId());
                processedCount++;
            } catch (Exception e) {
                log.error("발언 동기화 중 오류 발생: statementId={}, error={}", statement.getId(), e.getMessage());
            }
        }
        log.info("발언 데이터 타임라인 동기화 완료: 처리={}, 스킵={}, 전체={}", processedCount, skippedCount, statements.size());
    }

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

    private void validateKeyword(String keyword) {

    }


    private TimelineEventDocument createStatementEvent(StatementDocument statement, String statementId) {
        return null;
    }

    private Optional<TimelineEventDocument> findExistingEvent(String sourceTypeStatement, String statementId) {
        return null;
    }

    private StatementDocument getStatementOrThrow(String statementId) {
        return null;
    }

    private TimelineEventDocument createBillEvent(ProposedBillResponse billResponse, Figure figure, String billId) {
        return null;
    }

    /**
     *
     * @param figureId
     * @return
     */
    private Figure getFigureOrThrow(Long figureId) {
        return null;
    }

    private TimelineEventDocument createCustomEvent(Figure figure, String description, LocalDate eventDate, TimelineEventType type) {
        return null;
    }

    private void validateCustomEventParameters(String title, String description, LocalDate eventDate, TimelineEventType type) {

    }
}
