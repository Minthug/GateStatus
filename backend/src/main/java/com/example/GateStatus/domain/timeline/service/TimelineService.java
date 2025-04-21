package com.example.GateStatus.domain.timeline.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.service.ProposedBillService;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.timeline.repository.TimelineEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다"));

        return timelineRepository.findByFigureIdOrderByEventDateDesc(figureId, pageable)
                .map(TimelineEventResponse::from);
    }
}
