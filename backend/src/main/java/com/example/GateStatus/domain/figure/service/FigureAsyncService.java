package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.common.SyncJobStatus;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
//
//
//    /**
//     *
//     * @param figure
//     * @return
//     */
//    private boolean processOneFigure(FigureInfoDTO figure) {
//
//        try {
//            Boolean result = transactionTemplate.execute(status -> {
//                try {
//                    // 이 블록 내에서 실행되는 모든 데이터베이스 작업은 트랜잭션 내에서 실행됩니다
//                    boolean exists = figureRepository.existsByFigureId(figure.figureId());
//                    Figure figureEntity;
//
//                    if (exists) {
//                        figureEntity = updateFigureBasicInfoJpa(figure);
//                    } else {
//                        figureEntity = insertFigureBasicInfoJpa(figure);
//                    }
//
//                    if (figureEntity != null) {
//                        updateCollectionsWithJpa(figureEntity, figure);
//                    }
//
//                    return true;
//                } catch (Exception e) {
//                    log.error("국회의원 저장 중 오류: {} - {}", figure.name(), e.getMessage(), e);
//                    status.setRollbackOnly();
//                    return false;
//                }
//            });
//
//            return Boolean.TRUE.equals(result);
//        } catch (Exception e) {
//            log.error("국회의원 트랜잭션 처리 중 예외 발생: {} - {}", figure.name(), e.getMessage(), e);
//            return false;
//        }
//    }
//
//
//    private void updateCollectionsWithJpa(Figure figure, FigureInfoDTO dto) {
//        try {
//            // 이하 기존 코드와 동일
//            // 컬렉션 초기화
//            if (figure.getEducation() == null) {
//                figure.setEducation(new ArrayList<>());
//            } else {
//                figure.getEducation().clear();
//            }
//
//            if (figure.getCareers() == null) {
//                figure.setCareers(new ArrayList<>());
//            } else {
//                figure.getCareers().clear();
//            }
//
//            if (figure.getSites() == null) {
//                figure.setSites(new ArrayList<>());
//            } else {
//                figure.getSites().clear();
//            }
//
//            if (figure.getActivities() == null) {
//                figure.setActivities(new ArrayList<>());
//            } else {
//                figure.getActivities().clear();
//            }
//
//            if (dto.education() != null) {
//                for (String education : dto.education()) {
//                    if (education != null && !education.trim().isEmpty()) {
//                        figure.getEducation().add(education.trim());
//                    }
//                }
//            }
//
//            if (dto.electedCount() != null && !dto.electedCount().isEmpty()) {
//                Career assemblyCareer = Career.builder()
//                        .title(dto.electedCount() + "대 국회의원")
//                        .position("국회의원")
//                        .organization("대한민국 국회")
//                        .period(dto.electedDate() != null ? dto.electedDate() + " ~ 현재" : "현재")
//                        .build();
//                figure.getCareers().add(assemblyCareer);
//            }
//
//            if (dto.committeeName() != null && !dto.committeeName().isEmpty()) {
//                String position = dto.committeePosition() != null ? dto.committeePosition() : "위원";
//                Career committeeCareer = Career.builder()
//                        .title("국회 " + dto.committeeName())
//                        .position(position)
//                        .organization(dto.committeeName())
//                        .period("현재")
//                        .build();
//                figure.getCareers().add(committeeCareer);
//            }
//
//            if (dto.career() != null && !dto.career().isEmpty()) {
//                figure.getCareers().addAll(dto.career());
//            }
//
//            if (dto.homepage() != null && !dto.homepage().trim().isEmpty()) {
//                figure.getSites().add(dto.homepage().trim());
//            }
//
//            // Add email as mailto: link if available
//            if (dto.email() != null && !dto.email().trim().isEmpty()) {
//                figure.getSites().add("mailto:" + dto.email().trim());
//            }
//
//            // Update activities collection
//            // Add elected info if available
//            if (dto.electedCount() != null && !dto.electedCount().isEmpty()) {
//                figure.getActivities().add(dto.electedCount() + "대 국회의원");
//            }
//
//            // Add committee info if available
//            if (dto.committeeName() != null && !dto.committeeName().isEmpty()) {
//                String position = dto.committeePosition() != null ? dto.committeePosition() : "위원";
//                figure.getActivities().add(dto.committeeName() + " " + position);
//            }
//
//            figureRepository.save(figure);
//            log.debug("컬렉션 정보 JPA 업데이트 완료: {}", dto.name());
//        } catch (Exception e) {
//            log.error("컬렉션 정보 JPA 업데이트 중 오류: {} - {}", dto.name(), e.getMessage(), e);
//            throw e;
//        }
//    }
}
