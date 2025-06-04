package com.example.GateStatus.domain.dashboard.service;

import com.example.GateStatus.domain.dashboard.dto.response.*;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureApiService;
import com.example.GateStatus.domain.figure.service.response.FigureDTO;
import com.example.GateStatus.domain.proposedBill.repository.ProposedBillRepository;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import com.example.GateStatus.domain.vote.repository.VoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FigureRepository figureRepository;
    private final ProposedBillRepository billRepository;
    private final StatementMongoRepository statementMongoRepository;
    private final VoteRepository voteRepository;
    private final FigureApiService figureApiService;
    private final DashboardStatisticsService statisticsService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardDataByName(String name) {
        Figure figure = findOrSyncFigure(name);
        return buildDashboardResponse(figure);
    }


    @Transactional(readOnly = true)
    public DashboardResponse getDashboardDataById(Long figureId) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다: " + figureId));

        return buildDashboardResponse(figure);
    }

    @Transactional(readOnly = true)
    public List<DashboardResponse> getMultipleDashboardData(List<Long> figureIds) {
        List<Figure> figures = figureRepository.findAllById(figureIds);

        if (figures.size() != figureIds.size()) {
            List<Long> foundIds = figures.stream().map(Figure::getId).toList();
            List<Long> missingIds = figureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            log.warn("일부 정치인을 찾을 수 없음: {}", missingIds);
        }

        return figures.stream()
                .map(this::buildDashboardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardDataByPeriod(Long figureId, LocalDate startDate, LocalDate endDate) {
        Figure figure = figureRepository.findById(figureId)
                .orElseThrow(() -> new EntityNotFoundException("해당 정치인이 존재하지 않습니다 " + figureId));

        return buildDashboardResponseForPeriod(figure, startDate, endDate);
    }

    // ===== Private Helper Methods =====

    private DashboardResponse buildDashboardResponse(Figure figure) {
        return buildDashboardResponseForPeriod(figure, null, null);
    }

    private Figure findOrSyncFigure(String name) {
        return figureRepository.findByName(name)
                .orElseGet(() -> {
                    try {
                        log.info("DB에 없어 API에서 정치인 정보 동기화 시도: {}", name);
                        return figureApiService.syncFigureInfoByName(name);
                    } catch (Exception e) {
                        log.error("API에서 정치인 정보 동기화 실패: {} - {}", name, e.getMessage());
                        throw new EntityNotFoundException("해당 정치인을 찾을 수 없습니다: " + name);
                    }
                });
    }

    private DashboardResponse buildDashboardResponseForPeriod(Figure figure, LocalDate startDate, LocalDate endDate) {
        Long figureId = figure.getId();

        CompletableFuture<BillStatistics> billStatsFuture =
                CompletableFuture.supplyAsync(() -> statisticsService.getBillStatistics(figureId, startDate, endDate));

        CompletableFuture<StatementStatistics> statementStatsFuture =
                CompletableFuture.supplyAsync(() -> statisticsService.getStatementStatistics(figureId, startDate, endDate));

        CompletableFuture<VoteStatistics> voteStatsFuture =
                CompletableFuture.supplyAsync(() -> statisticsService.getVoteStatistics(figureId, startDate, endDate));

        CompletableFuture<List<BillOverTimeDTO>> billsOverTimeFuture =
                CompletableFuture.supplyAsync(() -> statisticsService.getBillOverTime(figureId, startDate, endDate));

        CompletableFuture<List<KeywordDTO>> keywordsFuture =
                CompletableFuture.supplyAsync(() -> statisticsService.getKeywords(figureId, startDate, endDate));

        try {
            CompletableFuture.allOf(billStatsFuture, statementStatsFuture, voteStatsFuture,
                    billsOverTimeFuture, keywordsFuture).join();

            return new DashboardResponse(
                    FigureDTO.from(figure),
                    billStatsFuture.get(),
                    statementStatsFuture.get(),
                    voteStatsFuture.get(),
                    keywordsFuture.get(),
                    billsOverTimeFuture.get());
        } catch (Exception e) {
            log.error("대시보드 데이터 구성 중 오류 발생: figureId={}", figureId, e);
            throw new RuntimeException("대시보드 데이터를 구성할 수 없습니다", e);
        }
    }
}
