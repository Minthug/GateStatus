package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BillAsyncService {

    private final ProposedBillApiService proposedBillApiService;
    private final FigureRepository figureRepository;

    public BillAsyncService(ProposedBillApiService proposedBillApiService, FigureRepository figureRepository) {
        this.proposedBillApiService = proposedBillApiService;
        this.figureRepository = figureRepository;
    }

    /**
     * 특정 제안자(국회의원)의 법안을 비동기적으로 동기화합니다.
     * @param proposerName
     * @return
     */
    @Async
    public CompletableFuture<Integer> syncBillsByProposerAsync(String proposerName) {
        int count = proposedBillApiService.syncBillByProposer(proposerName);
        return CompletableFuture.completedFuture(count);
    }

    /**
     * 모든 국회의원의 법안을 비동기적으로 동기화합니다.
     * @return
     */
    @Async
    public CompletableFuture<Integer> syncAllBillsAsync() {
        // 국회의원 목록
        List<Figure> figures = figureRepository.findByFigureType(FigureType.POLITICIAN);

        // 각 국회의원별로 비동기 작업 시작
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (Figure figure : figures) {
            CompletableFuture<Integer> future = syncBillsByProposerAsync(figure.getName());
            futures.add(future);
        }

        // 모든 작업 완료 대기 및 결과 집계
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .mapToInt(Integer::intValue)
                        .sum());
    }


    public CompletableFuture<Integer> syncBillsByFiguresAsync(List<Figure> figures) {
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (Figure figure : figures) {
            CompletableFuture<Integer> future = syncBillsByProposerAsync(figure.getName());
            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .mapToInt(Integer::intValue)
                        .sum());
    }
}
