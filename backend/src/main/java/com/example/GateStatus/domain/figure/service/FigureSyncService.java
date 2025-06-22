package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.career.Career;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class FigureSyncService {

    private final AssemblyApiClient apiClient;
    private final FigureRepository figureRepository;
    private final FigureMapper mapper;
    private final FigureCacheService cacheService;
    private final FigureMapper figureMapper;
    private final EntityManager entityManager;

    @Transactional
    public Figure syncFigureByName(String figureName) {
        log.info("국회의원 정보 동기화 시작: {}", figureName);

        FigureInfoDTO info = apiClient.fetchFigureByName(figureName);
        if (info == null) {
            throw new EntityNotFoundException("해당 이름의 정치인을 찾을 수 없습니다: " + figureName);
        }

        Figure figure = figureRepository.findByName(figureName)
                .orElseGet(() -> createNewFigure(figureName));

        mapper.updateFigureFromDTO(figure, info);
        Figure savedFigure= figureRepository.save(figure);

        if (savedFigure.getFigureId() != null) {
            cacheService.updateFigureCache(savedFigure);
        }

        log.info("국회의원 정보 동기화 완료: {}", figureName);
        return savedFigure;
    }

    @Transactional
    public int syncAllFigures() {
        log.info("모든 국회의원 정보 동기화 시작 ");

        List<FigureInfoDTO> allFigures = apiClient.fetchAllFigures();
        if (allFigures.isEmpty()) {
            log.warn("동기화할 국회의원 정보가 없습니다");
            return 0;
        }

        int successCount = 0;
        for (FigureInfoDTO figureInfo : allFigures) {
            try {
                syncSingleFigure(figureInfo);
                successCount++;
            } catch (Exception e) {
                log.error("국회의원 동기화 실패: {} - {}", figureInfo.name(), e.getMessage());
            }
        }

        log.info("모든 국회의원 정보 동기화 완료: 총 {}명 중 {}명 성공",
                allFigures.size(), successCount);
        return successCount;
    }

    @Transactional
    public int syncFiguresByParty(String partyName) {
        log.info("{}당 소속 국회의원 정보 동기화 시작", partyName);

        List<FigureInfoDTO> partyFigures = apiClient.fetchFiguresByParty(partyName);
        int successCount = 0;

        for (FigureInfoDTO infoDTO : partyFigures) {
            try {
                syncSingleFigure(infoDTO);
                successCount++;
            } catch (Exception e) {
                log.error("국회의원 동기화 실패: {} - {}", infoDTO.name(), e.getMessage());
            }
        }
        log.info("{}당 소속 국회의원 정보 동기화 완료: {}명", partyName, successCount);
        return successCount;
    }

    private Figure createNewFigure(String name) {
        return Figure.builder()
                .name(name)
                .figureType(FigureType.POLITICIAN)
                .viewCount(0L)
                .build();
    }

    public void syncSingleFigure(FigureInfoDTO figureInfoDTO) {
        Figure figure = figureRepository.findByFigureId(figureInfoDTO.figureId())
                .orElseGet(() -> createNewFigure(figureInfoDTO.name()));

        figureMapper.updateFigureFromDTO(figure, figureInfoDTO);
        figureRepository.save(figure);
    }

    /**
     * 단일 국회의원 저장 메서드 (별도 트랜잭션으로 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveBasicFigureInfo(FigureInfoDTO figureDTO) {
        try {
            log.info("국회의원 저장 시작: {}", figureDTO.name());

            // 1. 기존 엔티티 조회 또는 새로 생성
            Figure figure = figureRepository.findByFigureIdWithoutCollections(figureDTO.figureId())
                    .orElseGet(() ->
                            Figure.builder()
                                    .figureId(figureDTO.figureId())
                                    .name(figureDTO.name())
                                    .figureType(FigureType.POLITICIAN)
                                    .viewCount(0L)
                                    .build());

            // 기본 정보만 업데이트
            figure.setName(figureDTO.name());
            figure.setEnglishName(figureDTO.englishName());
            figure.setBirth(figureDTO.birth());
            figure.setConstituency(figureDTO.constituency());
            figure.setFigureParty(figureDTO.partyName());
            figure.setUpdateSource("국회 Open API");

            // 저장
            Figure savedFigure = figureRepository.save(figure);

            // EntityManager로 직접 조회
            boolean exists = false;
            try {
                Figure found = entityManager.createQuery(
                                "SELECT f FROM Figure f WHERE f.figureId = :id", Figure.class)
                        .setParameter("id", figureDTO.figureId())
                        .getSingleResult();
                exists = (found != null);
            } catch (Exception e) {
                log.warn("엔티티 조회 실패: {}", e.getMessage());
            }

            if (exists) {
                log.info("국회의원 저장 성공: {}, ID={}", figureDTO.name(), figureDTO.figureId());
                return true;
            } else {
                log.warn("국회의원 저장 실패 (직접 조회 불가): {}", figureDTO.name());
                return false;
            }
        } catch (Exception e) {
            log.error("국회의원 저장 실패: {} - {}", figureDTO.name(), e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 다시 던지기
        }
    }


    /**
     * 교육 정보 업데이트
     */
    private void updateEducation(FigureInfoDTO figure) {
        try {
            if (figure.education() == null || figure.education().isEmpty()) {
                return;
            }

            Figure entity = figureRepository.findByFigureIdWithEducation(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 기존 교육 정보 비우고 새로 추가
            entity.getEducation().clear();
            entity.getEducation().addAll(figure.education());

            figureRepository.save(entity);
            log.debug("교육 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("교육 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
            // 다음 컬렉션 처리를 위해 예외를 던지지 않음
        }
    }

    /**
     * 경력 정보 업데이트
     */
    private void updateCareers(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithCareers(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 컬렉션이 null인 경우 대비
            if (entity.getCareers() == null) {
                entity.setCareers(new ArrayList<>());
            } else {
                entity.getCareers().clear();
            }

            // 새 경력 정보 목록 생성
            List<Career> newCareers = new ArrayList<>();

            // DTO의 경력 정보 추가 (타입 안전하게 처리)
            if (figure.career() != null && !figure.career().isEmpty()) {
                try {
                    // Object 타입이므로 안전하게 변환
                    for (Object careerObj : figure.career()) {
                        if (careerObj instanceof Career) {
                            Career career = (Career) careerObj;
                            newCareers.add(Career.builder()
                                    .title(career.getTitle())
                                    .position(career.getPosition())
                                    .organization(career.getOrganization())
                                    .period(career.getPeriod())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.warn("경력 정보 변환 중 오류: {}", e.getMessage());
                    // 변환 오류가 있어도 계속 진행
                }
            }

            // 국회의원 기본 경력 정보 추가
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                Career career = Career.builder()
                        .title(figure.electedCount() + "대 국회의원")
                        .position("국회의원")
                        .organization("대한민국 국회")
                        .period(figure.electedDate() != null ? figure.electedDate() + " ~ 현재" : "")
                        .build();
                entity.getCareers().add(career);
            }

            // 위원회 경력 정보 추가
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                Career career = Career.builder()
                        .title("국회 " + figure.committeeName())
                        .position(position)
                        .organization(figure.committeeName())
                        .period("현재")
                        .build();
                newCareers.add(career);
            }

            entity.getCareers().addAll(newCareers);

            figureRepository.save(entity);
            log.debug("경력 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("경력 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }

    /**
     * 사이트 정보 업데이트
     */
    private void updateSites(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithSites(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));

            // 기존 사이트 정보 비우기
            entity.getSites().clear();

            // 새 사이트 정보 추가
            if (figure.homepage() != null && !figure.homepage().trim().isEmpty()) {
                entity.getSites().add(figure.homepage().trim());
            }

            if (figure.email() != null && !figure.email().trim().isEmpty()) {
                entity.getSites().add("mailto:" + figure.email().trim());
            }

            figureRepository.save(entity);
            log.debug("사이트 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("사이트 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }

    /**
     * 활동 정보 업데이트
     */
    private void updateActivities(FigureInfoDTO figure) {
        try {
            Figure entity = figureRepository.findByFigureIdWithActivities(figure.figureId())
                    .orElseThrow(() -> new EntityNotFoundException("국회의원을 찾을 수 없습니다: " + figure.figureId()));


            // 컬렉션이 null인 경우 대비
            if (entity.getSites() == null) {
                entity.setSites(new ArrayList<>());
            } else {
                entity.getSites().clear();
            }

            // 새 활동 정보 목록 생성
            List<String> newActivities = new ArrayList<>();

            // 당선 정보 추가
            if (figure.electedCount() != null && !figure.electedCount().isEmpty()) {
                newActivities.add(figure.electedCount() + "대 국회의원");
            }

            // 위원회 정보 추가
            if (figure.committeeName() != null && !figure.committeeName().isEmpty()) {
                String position = figure.committeePosition() != null ? figure.committeePosition() : "위원";
                newActivities.add(figure.committeeName() + " " + position);
            }

            // 컬렉션에 추가
            entity.getActivities().addAll(newActivities);

            // 저장
            figureRepository.save(entity);

            log.debug("활동 정보 업데이트 완료: {}", figure.name());
        } catch (Exception e) {
            log.warn("활동 정보 업데이트 중 오류: {} - {}", figure.name(), e.getMessage());
        }
    }
}
