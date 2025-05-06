package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillResponse;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.statement.entity.Statement;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final TimelineEventRepository timelineRepository;
    private final FigureRepository figureRepository;
    private final StatementMongoRepository statementRepository;
    private final ProposedBillService billService;

    /**
     * 특정 정치인의 타임라인 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> getFigureTimeline(Long figureId, Pageable pageable) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

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
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

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
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

        return timelineRepository.findByFigureIdAndDateRange(figureId, startDate, endDate, pageable)
                .map(TimelineEventResponse::from);
    }

    /**
     * 특정 키워드 포함하는 타임라인 검색
     * @param figureId
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> searchFigureTimeline(Long figureId, String keyword, Pageable pageable) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

        return timelineRepository.searchByKeyword(figureId, keyword, pageable)
                .map(TimelineEventResponse::from);
    }

    /**
     * 발언 데이터를 타임라인 이벤트로 추가
     * @param statementId
     * @return
     */
    @Transactional
    public TimelineEventResponse addStatementToTimeline(String statementId) {
        StatementDocument statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언이 존재하지 않습니다" + statementId));

        if (timelineRepository.existsBySourceTypeAndSourceId("STATEMENT", statementId)) {
            log.debug("이미 타임라인에 등록된 발언입니다: {}", statementId);

            TimelineEventDocument event = timelineRepository.findBySourceTypeAndSourceId("STATEMENT", statementId).get(0);

            return TimelineEventResponse.from(event);
        }

        TimelineEventDocument timelineEvent = TimelineEventDocument.builder()
                .figureId(statement.getFigureId())
                .figureName(statement.getFigureName())
                .eventDate(statement.getStatementDate())
                .title(statement.getTitle())
                .description(statement.getContent())
                .eventType(TimelineEventType.STATEMENT)
                .sourceType("STATEMENT")
                .sourceId(statementId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TimelineEventDocument savedEvent = timelineRepository.save(timelineEvent);
        return TimelineEventResponse.from(savedEvent);
    }


    /**
     * 법안 데이터를 타임라인 이벤트로 추가
     * @param billId
     * @param figureId
     * @return
     */
    @Transactional
    public TimelineEventResponse addBillToTimeline(String billId, Long figureId) {
        ProposedBillResponse billResponse = billService.findBillById(billId);

        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

        if (timelineRepository.existsBySourceTypeAndSourceId("BILL", billResponse.toString())) {
            log.debug("이미 타임라인에 등록된 법안입니다: {}", billId);

            TimelineEventDocument event = timelineRepository.findBySourceTypeAndSourceId("BILL", billResponse.toString()).get(0);
            return TimelineEventResponse.from(event);
        }

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("billStatus", billResponse.billStatus().toString());
        additionalData.put("proposerCount", billResponse.coProposers().size() + 1);

        TimelineEventDocument timelineEvent = TimelineEventDocument.builder()
                .figureId(figureId)
                .figureName(figure.getName())
                .eventDate(billResponse.processDate())
                .title(billResponse.billName())
                .description("법안 발의: " + billResponse.billName())
                .eventType(TimelineEventType.BILL_PROPOSED)
                .sourceType("BILL")
                .sourceId(billId.toString())
                .additionalData(additionalData)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TimelineEventDocument savedEvent = timelineRepository.save(timelineEvent);
        return TimelineEventResponse.from(savedEvent);
    }


    @Transactional
    public TimelineEventResponse addCustomEvent(Long figureId, String title, String description,
                                                LocalDate eventDate, TimelineEventType type) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다" + figureId));

        TimelineEventDocument event = TimelineEventDocument.builder()
                .figureId(figureId)
                .figureName(figure.getName())
                .eventDate(eventDate)
                .title(title)
                .description(description)
                .eventType(type)
                .sourceType("CUSTOM")
                .sourceId(null) // 커스텀 이벤트는 소스 ID 없음
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TimelineEventDocument savedEvent = timelineRepository.save(event);
        return TimelineEventResponse.from(savedEvent);
    }

    /**
     * 발언 데이터를 자동으로 타임라인에 동기화
     * 매일 자정 실행
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void syncStatementsToTimeline() {
        log.info("발언 데이터 타임라인 동기화 시작");

        LocalDate startDate = LocalDate.now().minusWeeks(1);
        List<StatementDocument> statements = statementRepository.findByPeriod(startDate, LocalDate.now());

        int count = 0;
        for (StatementDocument statement : statements) {
            if (timelineRepository.existsBySourceTypeAndSourceId("STATEMENT", statement.getId())) {
                continue;
            }

            addStatementToTimeline(statement.getId());
            count++;
        }

        log.info("발언 데이터 타임라인 동기화 완료: {} 건 처리", count);
    }

    @Transactional
    public void deleteTimelineEvent(String eventId) {
        TimelineEventDocument event = timelineRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 타임라인 이벤트가 존재하지 않습니다 " + eventId));

        timelineRepository.delete(event);
        log.info("타임라인 이벤트 삭제: {}", eventId);
    }
}
