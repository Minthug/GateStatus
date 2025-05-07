package com.example.GateStatus.domain.proposedBill.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.proposedBill.ProposedBillApiService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class ProposedBillQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final FigureRepository figureRepository;
    private final ProposedBillApiService proposedBillApiService;

    private final ConcurrentMap<String, JobStatus> jobStatusMap = new ConcurrentHashMap<>();

    @Value("${app.queue.exchange}")
    private String exchange;

    @Value("${app.queue.routing-key}")
    private String routingKey;

    @Value("${app.queue.batch-size}")
    private int batchSize;

    public ProposedBillQueueService(RabbitTemplate rabbitTemplate, FigureRepository figureRepository, ProposedBillApiService proposedBillApiService) {
        this.rabbitTemplate = rabbitTemplate;
        this.figureRepository = figureRepository;
        this.proposedBillApiService = proposedBillApiService;
    }

    /**
     * 단일 국회의원의 법안 동기화 작업을 큐에 추가합니다.
     * @param proposerName
     * @param jobId
     */
    public void queueBillsSyncTask(String proposerName, String jobId) {
        SyncTask task = new SyncTask(jobId, proposerName);
        rabbitTemplate.convertAndSend(exchange, routingKey, task);
        log.info("국회의원 {} 법안 동기화 작업을 큐에 추가했습니다. (작업 ID: {})", proposerName, jobId);
    }


    /**
     * 모든 국회의원의 법안 동기화 작업을 큐에 추가하고 작업 ID를 반환합니다.
     */
    public String queueAllBillsSyncTask() {
        String jobId = UUID.randomUUID().toString();
        List<Figure> figures = figureRepository.findByFigureType(FigureType.POLITICIAN);

        JobStatus status = new JobStatus(jobId, figures.size());
        jobStatusMap.put(jobId, status);

        List<List<Figure>> batches = splitIntoBatches(figures, batchSize);

        for (List<Figure> batch : batches) {
            for (Figure figure : batch) {
                queueBillsSyncTask(figure.getName(), jobId);
            }
        }

        log.info("총 {}명의 국회의원 법안 동기화 작업을 큐에 추가했습니다. (작업 ID: {})", figures.size(), jobId);
        return jobId;
    }

    @RabbitListener(queues = "bill-sync-queue")
    public void processBillSyncTask(SyncTask task) {
        String proposerName = task.getProposerName();
        String jobId = task.getJobId();

        try {
            log.info("국회의원 {} 법안 동기화 작업 시작 (작업 ID: {})", proposerName, jobId);
            int count = proposedBillApiService.syncBillByProposer(proposerName);

            if (jobId != null && jobStatusMap.containsKey(jobId)) {
                JobStatus status = jobStatusMap.get(jobId);
                status.incrementCompleted();
                status.addSyncCount(count);

                log.info("국회의원 {} 법안 동기화 완료: {}건 (진행률: {}%)",
                        proposerName, count, status.getProgressPercentage());
            }
        } catch (Exception e) {
            log.error("국회의원 {} 법안 동기화 실패: {}", proposerName, e.getMessage(), e);
            // 실패 시 재시도 로직 추가 가능
        }
    }

    /**
     * 작업 상태를 조회
     * @param jobId
     * @return
     */
    public JobStatus getJobStatus(String jobId) {
        return jobStatusMap.get(jobId);
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

    /**
     * 작업 상태를 저장하는 클래스
     */
    @Data
    public static class JobStatus {
        private final String jobId;
        private final int totalTasks;
        private int completedTasks;
        private int syncCount;
        private LocalDateTime startTime = LocalDateTime.now();
        private LocalDateTime endTime;

        public void incrementCompleted() {
            completedTasks++;
            if (completedTasks >= totalTasks) {
                endTime = LocalDateTime.now();
            }
        }

        public void addSyncCount(int count) {
            syncCount += count;
        }

        public int getProgressPercentage() {
            return (int) ((completedTasks * 100.0) / totalTasks);
        }

        public boolean isCompleted() {
            return completedTasks >= totalTasks;
        }

        public Duration getElapsedTime() {
            LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
            return Duration.between(startTime, end);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SyncTask implements Serializable {
        private String jobId;
        private String proposerName;
    }
}
