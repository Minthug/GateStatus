package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureApiService {

    private final AssemblyApiClient apiClient;
    private final FigureSyncService syncService;
    private final FigureAsyncService asyncService;
    private final FigureRepository figureRepository;
    private final FigureCacheService cacheService;

    @Transactional
    public Figure syncFigureInfoByName(String figureName) {
        log.info("국회의원 정보 동기화 요청: {}", figureName);

        try {
            Figure result = syncService.syncFigureByName(figureName);
            log.info("국회의원 정보 동기화 성공: {}", figureName);
            return result;
        } catch (Exception e) {
            log.error("국회의원 정보 동기화 실패: {} - {}", figureName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 모든 국회의원 정보를 동기화 합니다
     *
     * @return
     */
    public int syncAllFiguresV3() {
        log.info("모든 국회의원 정보 동기화 시작");

        try {
            int successCount = syncService.syncAllFigures();
            log.info("모든 국회의원 정보 동기화 완료 (V3): {}명 성공", successCount);
            return successCount;
        } catch (Exception e) {
            log.error("모든 국회의원 정보 동기화 실패 (V3): {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 모든 국회의원 정보를 비동기적으로 동기화합니다
     *
     * @return 작업 ID (상태 추적용)
     */
    public String syncAllFiguresV4() {
        log.info("모든 국회의원 정보 동기화 시작 V4");

        try {
            String jobId = asyncService.syncAllFiguresAsync();
            log.info("모든 국회의원 정보 비동기 동기화 작업 시작됨 (V4): jobId={}", jobId);

            return jobId;
        } catch (Exception e) {
            log.error("모든 국회의원 정보 비동기 동기화 시작 실패 (V4): {}", e.getMessage(), e);

            throw e;
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 동기화합니다
     *
     * @param partyName
     * @return
     */
    @Transactional
    public int syncFigureByParty(String partyName) {
        log.info("{}당 소속 국회의원 정보 동기화 시작", partyName);

        try {
            int successCount = syncService.syncFiguresByParty(partyName);
            log.info("{}당 소속 국회의원 정보 동기화 완료: {}명 성공", partyName, successCount);
            return successCount;
        } catch (Exception e) {
            log.error("{}당 소속 국회의원 정보 동기화 실패: {}", partyName, e.getMessage(), e);
            throw e;
        }
    }

//    @Transactional
//    public Figure syncSingleFigure(Figure figure, FigureInfoDTO infoDTO) {
//        log.info("단일 국회의원 정보 동기화: {}", infoDTO.name());
//
//        try {
//            Figure result = syncService.syncSingleFigure(figure, infoDTO);
//            log.info("단일 국회의원 정보 동기화 성공: {}", infoDTO.name());
//            return result;
//        } catch (Exception e) {
//            log.error("단일 국회의원 정보 동기화 실패: {} - {}", infoDTO.name(), e.getMessage(), e);
//            throw e;
//        }
//    }


    // ========== 통계 및 모니터링 메서드들 ==========

    /**
     * API 호출 상태 확인
     *
     * @return API 서버 응답 가능 여부
     */
    public boolean checkApiHealth() {
        log.debug("API 서버 상태 확인");

        try {
            // 간단한 API 호출로 상태 확인
            List<FigureInfoDTO> testResult = apiClient.fetchFiguresPage(1, 1);
            boolean isHealthy = testResult != null;
            log.debug("API 서버 상태: {}", isHealthy ? "정상" : "비정상");
            return isHealthy;
        } catch (Exception e) {
            log.warn("API 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }


    /**
     * 동기화 가능한 총 국회의원 수 조회
     *
     * @return 총 국회의원 수
     */
    public int getTotalAvailableFiguresCount() {
        log.debug("동기화 가능한 총 국회의원 수 조회");

        try {
            List<FigureInfoDTO> allFigures = apiClient.fetchAllFigures();
            int count = allFigures.size();
            log.debug("동기화 가능한 총 국회의원 수: {}명", count);
            return count;
        } catch (Exception e) {
            log.error("총 국회의원 수 조회 실패: {}", e.getMessage(), e);
            return 0;
        }
    }

    public Map<String, Object> getSyncStatus() {
        log.debug("동기화 상태 정보 조회");

        Map<String, Object> status = new HashMap<>();

        try {
            long dbCount = figureRepository.count();
            int apiCount = getTotalAvailableFiguresCount();

            status.put("dbCount", dbCount);
            status.put("apiCount", apiCount);
            status.put("syncNeeded", apiCount > dbCount);
            status.put("syncDifference", apiCount - dbCount);
            status.put("lastChecked", LocalDateTime.now());

            log.debug("동기화 상태: DB={}명, API={}명, 차이={}명",
                    dbCount, apiCount, (apiCount - dbCount));
        } catch (Exception e) {
            log.error("동기화 상태 정보 조회 실패: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
        }
        return status;
    }

    // ========== 캐시 관련 편의 메서드들 ==========

    public void refreshFigureCache(String figureId) {
        log.info("국회의원 캐시 갱신: figureId={}", figureId);

        try {
            Figure figure = figureRepository.findByFigureId(figureId)
                    .orElseThrow(() -> new EntityNotFoundException("Figure not found: " + figureId));

            cacheService.updateFigureCache(figure);
            log.info("국회의원 캐시 갱신 완료: figureId={} ", figureId);
        } catch (Exception e) {
            log.error("국회의원 캐시 갱신 실패: figureId={}, 오류={}", figureId, e.getMessage(), e);
            throw e;
        }
    }

    public void clearAllFigureCache() {
        log.info("모든 국회의원 캐시 삭제");

        try {
            cacheService.evictFigureCache("figure:*");
            log.info("모든 국회의원 캐시 삭제 완료");

        } catch (Exception e) {
            log.error("모든 국회의원 캐시 삭제 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}


