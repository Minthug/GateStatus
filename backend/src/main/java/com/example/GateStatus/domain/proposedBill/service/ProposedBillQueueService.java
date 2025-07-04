package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProposedBillQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final FigureRepository figureRepository;
    private final ProposedBillApiService proposedBillApiService;

    private final ConcurrentMap<String, SyncJobStatus> jobStatusMap = new ConcurrentHashMap<>();

    @Value("${app.queue.exchange}")
    private String exchange;

    @Value("${app.queue.routing-key}")
    private String routingKey;

    @Value("${app.queue.batch-size:10}")
    private int batchSize;

    /**
     * 단일 국회의원의 법안 동기화 작업을 큐에 추가합니다.
     * @param proposerName
     * @param jobId
     */
    public String queueBillsSyncTask(String proposerName, String jobId) {

        validateProposerName(proposerName);

        if (jobId == null) {
            jobId = UUID.randomUUID().toString();
        }

        SyncTask task = new SyncTask(jobId, proposerName);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, task);
            log.info("국회의원 {} 법안 동기화 작업을 큐에 추가했습니다. (작업 ID: {})", proposerName, jobId);
            return jobId;
        } catch (Exception e) {
            log.error("큐에 작업 추가 실패: 국회의원={}, jobId={}", proposerName, jobId, e);
            throw new RuntimeException("동기화 작업 큐 추가 실패", e);
        }
    }
    /**
     * 모든 국회의원의 법안 동기화 작업을 큐에 추가하고 작업 ID를 반환합니다.
     */
    public String queueAllBillsSyncTask() {
        String jobId = UUID.randomUUID().toString();

        try {
            List<Figure> figures = figureRepository.findByFigureType(FigureType.POLITICIAN);

            if (figures.isEmpty()) {
                log.warn("동기화할 국회의원이 없습니다.");
                return jobId;
            }

            SyncJobStatus status = new SyncJobStatus(jobId);
            status.setTotalTasks(figures.size());
            jobStatusMap.put(jobId, status);

            List<List<Figure>> batches = splitIntoBatches(figures, batchSize);

            for (List<Figure> batch : batches) {
                for (Figure figure : batch) {
                    queueBillsSyncTask(figure.getName(), jobId);
                }
            }

        log.info("총 {}명의 국회의원 법안 동기화 작업을 큐에 추가했습니다. (작업 ID: {})", figures.size(), jobId);
        return jobId;
        } catch (Exception e) {
            log.error("전체 법안 동기화 작업 큐 추가 실패: jobId={}", jobId, e);

            SyncJobStatus status = jobStatusMap.get(jobId);
            if (status != null) {
                status.setError(true);
                status.setErrorMessage("작업 큐 추가 실패: " + e.getMessage());
                status.setEndTime(LocalDateTime.now());
            }

            throw new RuntimeException("전체 동기화 작업 실패", e);
        }
    }

    @RabbitListener(queues = "bill-sync-queue")
    public void processBillSyncTask(SyncTask task) {
        String proposerName = task.getProposerName();
        String jobId = task.getJobId();

        log.info("국회의원 {} 법안 동기화 작업 시작 (작업 ID: {})", proposerName, jobId);
        SyncJobStatus status = jobStatusMap.get(jobId);

        try {
            int count = proposedBillApiService.syncBillByProposer(proposerName);

            if (status != null) {
                status.incrementCompletedTasks();
                status.incrementSuccessCount();
                status.addSyncCount(count);

                if (status.isAllTasksCompleted()) {
                    status.setCompleted(true);
                    status.setEndTime(LocalDateTime.now());
                    log.info("작업 ID {} 모든 동기화 완료", jobId);
                }
            }
                log.info("국회의원 {} 법안 동기화 완료: {}건 (진행률: {}%)",
                        proposerName, count, status != null ? status.getProgressPercentage() : 0);
        } catch (Exception e) {
            log.error("국회의원 {} 법안 동기화 실패: {}", proposerName, e.getMessage(), e);

            if (status != null) {
                status.incrementSuccessCount();
                status.incrementFailCount();
                status.setErrorMessage("부분 실패: " + e.getMessage());

                if (status.isAllTasksCompleted()) {
                    status.setCompleted(true);
                    status.setEndTime(LocalDateTime.now());

                    if (status.getFailCount() > 0) {
                        status.setError(true);
                    }
                }
            }
        }
    }

    public SyncJobStatus getJobStatus(String jobId) {
        validateJobId(jobId);
        return jobStatusMap.get(jobId);
    }

    @Scheduled(fixedRate = 3600000)
    public int cleanCompletedJob(int olderThanHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(olderThanHours);

        List<String> toRemove = jobStatusMap.entrySet().stream()
                .filter(entry -> {
                    SyncJobStatus status = entry.getValue();
                    return status.isCompleted() &&
                            status.getEndTime() != null &&
                            status.getEndTime().isBefore(cutoff);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(jobStatusMap::remove);

        if (!toRemove.isEmpty()) {
            log.info("완료된 작업 {} 개 정리 완료", toRemove.size());
        }
        return toRemove.size();
    }

    /**
     * 작업 목록 분할
     * @param items
     * @param batchSize
     * @return
     */
    private List<List<Figure>> splitIntoBatches(List<Figure> items, int batchSize) {
        List<List<Figure>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            batches.add(items.subList(i, Math.min(items.size(), i + batchSize)));
        }

        return batches;
    }


    // === Validation Methods ===

    private void validateProposerName(String proposerName) {
        if (proposerName == null || proposerName.trim().isEmpty()) {
            throw new IllegalArgumentException("국회의원 이름은 필수입니다");
        }
    }

    private void validateJobId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("작업 ID는 필수입니다");
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SyncTask implements Serializable {
        private static final long serialVersionUID = 1L;

        private String jobId;
        private String proposerName;
    }
}
