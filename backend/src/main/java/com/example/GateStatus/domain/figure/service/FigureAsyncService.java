package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
        log.info("국회의원 {} 정보 비동기 동기화 시작", figure.name());

        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            Boolean result = transactionTemplate.execute(status -> {
                try {
                    boolean exists = figureRepository.existsByFigureId(figure.figureId());

                    if (exists) {
                        updateFigureBasicInfo(figure);
                    } else {
                        insertFigureBasicInfo(figure);
                    }

                    updateCollectionsWithNativeSql(figure);

                    return true;
                } catch (Exception e) {
                    log.error("국회의원 저장 중 오류: {} - {}", figure.name(), e.getMessage(), e);
                    status.setRollbackOnly();
                    return false;
                }
            });

            if (Boolean.TRUE.equals(result)) {
                log.info("국회의원 {} 정보 동기화 성공", figure.name());
            } else {
                log.warn("국회의원 {} 정보 동기화 실패", figure.name());
            }

            return CompletableFuture.completedFuture(Boolean.TRUE.equals(result));
        } catch (Exception e) {
            log.error("국회의원 {} 처리 중 예외 발생: {}", figure.name(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private void updateFigureBasicInfo(FigureInfoDTO figure) {
        entityManager.createNativeQuery(
                        "UPDATE figure SET " +
                                "name = ?, " +
                                "english_name = ?, " +
                                "birth = ?, " +
                                "constituency = ?, " +
                                "figure_party = ?, " +
                                "update_source = ?, " +
                                "WHERE figure_id = ?")
                .setParameter(1, figure.name())
                .setParameter(2, figure.englishName())
                .setParameter(3, figure.birth())
                .setParameter(4, figure.constituency())
                .setParameter(5, figure.partyName() != null ? figure.partyName().toString() : null)
                .setParameter(6, "국회 Open API")
                .setParameter(7, figure.figureId())
                .executeUpdate();

        log.debug("국회의원 기본 정보 업데이트 완료: {}", figure.name());
    }



    private void insertFigureBasicInfo(FigureInfoDTO figure) {
        entityManager.createNativeQuery(
                        "INSERT INTO figure (figure_id, name, english_name, birth, constituency, figure_type, figure_party, view_count, update_source) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, figure.figureId())
                .setParameter(2, figure.name())
                .setParameter(3, figure.englishName())
                .setParameter(4, figure.birth())
                .setParameter(5, figure.constituency())
                .setParameter(6, "POLITICIAN")
                .setParameter(7, figure.partyName() != null ? figure.partyName().toString() : null)
                .setParameter(8, 0L)
                .setParameter(9, "국회 Open API")
                .executeUpdate();

        log.debug("새 국회의원 정보 삽입 완료: {}", figure.name());
    }

    private void updateCollectionsWithNativeSql(FigureInfoDTO figure) {
        String figureId = figure.figureId();

        try {
            entityManager.createNativeQuery("DELETE FROM figure_education WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_career WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_site WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM figure_activity WHERE figure_id = ?")
                    .setParameter(1, figureId)
                    .executeUpdate();

            if (figure.education() != null) {
                for (String education : figure.education()) {
                    if (education != null && !education.trim().isEmpty()) {
                        entityManager.createNativeQuery(
                                        "INSERT INTO figure_education (figure_id, education) VALUES (?, ?)")
                                .setParameter(1, figureId)
                                .setParameter(2, education.trim())
                                .executeUpdate();
                    }
                }
            }

            // 경력 정보 삽입
            // 국회의원 기본 경력
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_career (figure_id, title, position, organization, period) VALUES (?, ?, ?, ?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.electedCount() + "대 국회의원")
                        .setParameter(3, "국회의원")
                        .setParameter(4, "대한민국 국회")
                        .setParameter(5, figure.electedDate() != null ? figure.electedDate() + " ~ 현재" : "현재")
                        .executeUpdate();
            }

            // 위원회 경력
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                entityManager.createNativeQuery(
                                "INSERT INTO figure_career (figure_id, title, position, organization, period) VALUES (?, ?, ?, ?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, "국회 " + figure.committeeName())
                        .setParameter(3, position)
                        .setParameter(4, figure.committeeName())
                        .setParameter(5, "현재")
                        .executeUpdate();
            }

            // 사이트 정보 삽입
            if (figure.homepage() != null && !figure.homepage().trim().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_site (figure_id, site) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.homepage().trim())
                        .executeUpdate();
            }

            if (figure.email() != null && !figure.email().trim().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_site (figure_id, site) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, "mailto:" + figure.email().trim())
                        .executeUpdate();
            }

            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                entityManager.createNativeQuery(
                                "INSERT INTO figure_activity (figure_id, activity) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.electedCount() + "대 국회의원")
                        .executeUpdate();
            }

            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                entityManager.createNativeQuery(
                                "INSERT INTO figure_activity (figure_id, activity) VALUES (?, ?)")
                        .setParameter(1, figureId)
                        .setParameter(2, figure.committeeName() + " " + position)
                        .executeUpdate();
            }

            log.debug("컬렉션 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.error("컬렉션 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage(), e);
            throw e;
        }
    }

}
