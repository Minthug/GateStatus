package com.example.GateStatus.domain.figure.service.external;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FigureApiService {

    private final AssemblyApiClient apiClient;
    private final FigureSyncService syncService;
    private final FigureAsyncService asyncService;
    private final FigureRepository figureRepository;
//    private final FigureCacheService cacheService;

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

    // ========== 추가 편의 메서드들 ==========

    /**
     * 특정 이름의 국회의원 정보를 API에서 조회만 (동기화 안 함)
     *
     * @param figureName 국회의원 이름
     * @return 국회의원 정보 DTO (없으면 null)
     */
    public FigureInfoDTO fetchFigureByName(String figureName) {
        log.info("국회의원 정보 API 조회 (동기화 안 함): {}", figureName);

        try {
            FigureInfoDTO figure = apiClient.fetchFigureByName(figureName);
            if (figure != null) {
                log.info("국회의원 정보 API 조회 성공: {}", figureName);
            } else {
                log.warn("국회의원 정보 API 조회 결과 없음: {}", figureName);
            }
            return figure;
        } catch (Exception e) {
            log.error("국회의원 정보 API 조회 실패: {} - {}", figureName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 특정 정당 소속 국회의원 정보를 API에서 조회만 (동기화 안 함)
     *
     * @param partyName 정당명
     * @return 국회의원 정보 DTO 목록
     */
    public List<FigureInfoDTO> fetchFiguresByParty(String partyName) {
        log.info("{}당 소속 국회의원 정보 API 조회 (동기화 안 함)", partyName);

        try {
            List<FigureInfoDTO> figures = apiClient.fetchFiguresByParty(partyName);
            log.info("{}당 소속 국회의원 정보 API 조회 완료: {}명", partyName, figures.size());
            return figures;
        } catch (Exception e) {
            log.error("{}당 소속 국회의원 정보 API 조회 실패: {} - {}", partyName, e.getMessage(), e);
            throw e;
        }
    }
}


