package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FigureAsyncService {

    private final FigureApiService figureApiService;
    private final PlatformTransactionManager transactionManager;
    private final FigureRepository figureRepository;
    private final EntityManager entityManager;

    public FigureAsyncService(FigureApiService figureApiService, PlatformTransactionManager transactionManager, FigureRepository figureRepository, EntityManager entityManager) {
        this.figureApiService = figureApiService;
        this.transactionManager = transactionManager;
        this.figureRepository = figureRepository;
        this.entityManager = entityManager;
    }

    @Async
    public CompletableFuture<Integer> syncAllFiguresAsync() {
        log.info("모든 국회의원 정보 비동기 동기화 시작 ");
        List<FigureInfoDTO> allFigures = figureApiService.fetchAllFiguresFromAPiV2();

        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return CompletableFuture.completedFuture(0);
        }

        log.info("동기화 대상 국회의원: {}명", allFigures.size());

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (FigureInfoDTO figure : allFigures) {
            CompletableFuture<Boolean> future = syncSingleFigureAsync(figure);
            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            int successCount = (int) futures.stream()
                    .map(CompletableFuture::join)
                    .filter(result -> result)
                    .count();

            int failCount = allFigures.size() - successCount;

            log.info("국회의원 정보 비동기 동기화 완료: 총 {}명 중 {}명 성공, {}명 실패",
                    allFigures.size(), successCount, failCount);

            return successCount;
        });
    }

    @Async
    public CompletableFuture<Boolean> syncSingleFigureAsync(FigureInfoDTO figure) {
        return null;
    }
}
