package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
