package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureAsyncService {


    /**
     * 해당 서비스 클래스는 아마 추후 더 큰 확장성이 필요할 때
     * 기존 FigureApiService에 있는 Method를 대체하기 위한 비동기 처리 전용 서비스라고 생각한다
     */
    private final FigureApiService figureApiService;
    private final FigureRepository figureRepository;
    private final AssemblyApiClient apiClient;
    private final FigureSyncService syncService;

    private final ConcurrentMap<String, SyncJobStatus> jobStatusMap = new ConcurrentHashMap<>();

    public String syncAllFiguresAsync() {
        String jobId = UUID.randomUUID().toString();
        log.info("비동기 동기화 작업 시작: {}", jobId);

        CompletableFuture.runAsync(() -> processAsyncSyncJob(jobId));
        return jobId;
    }

    private void processAsyncSyncJob(String jobId) {
        SyncJobStatus jobStatus = new SyncJobStatus(jobId);
        jobStatusMap.put(jobId, jobStatus);

        try {
            List<FigureInfoDTO> allFigures = apiClient.fetchAllFigures();

            if (allFigures.isEmpty()) {
                jobStatus.setCompleted(true);
                jobStatus.setTotalTasks(0);
                return;
            }

            jobStatus.setTotalTasks(allFigures.size());

            int batchSize = 10;
            List<List<FigureInfoDTO>> batches = splitIntoBatches(allFigures, batchSize);

            int totalSuccess = 0;
            for (List<FigureInfoDTO> batch : batches) {
                totalSuccess += processBatch(batch, jobStatus);
            }
            // 작업 완료
            jobStatus.setSuccessCount(totalSuccess);
            jobStatus.setFailCount(allFigures.size() - totalSuccess);
            jobStatus.setCompleted(true);
            jobStatus.setEndTime(LocalDateTime.now());
        } catch (Exception e) {
            log.error("비동기 동기화 작업 실패: {} - {}", jobId, e.getMessage(), e);
            jobStatus.setError(true);
            jobStatus.setErrorMessage(e.getMessage());
            jobStatus.setCompleted(true);
        }

    }

    private int processBatch(List<FigureInfoDTO> batch, SyncJobStatus jobStatus) {
        int successCount = 0;

        for (FigureInfoDTO figure : batch) {
            try {
                syncSingleFigureInTransaction(figure);
                successCount++;
                jobStatus.incrementSuccessCount();
            } catch (Exception e) {
                log.error("배치 처리 중 오류: {} - {}", figure.name(), e.getMessage());
                jobStatus.incrementFailCount();
            } finally {
                jobStatus.incrementCompletedTasks();
            }
        }
        return successCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncSingleFigureInTransaction(FigureInfoDTO figure) {
        syncService.syncSingleFigure(figure);
    }


    private <T> List<List<T>> splitIntoBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(new ArrayList<>(
                    items.subList(i, Math.min(i + batchSize, items.size()))));
        }
        return batches;
    }
}
