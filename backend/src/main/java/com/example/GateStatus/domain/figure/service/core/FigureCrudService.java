package com.example.GateStatus.domain.figure.service.core;

import com.example.GateStatus.domain.common.JsonUtils;
import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.FigureMapper;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FigureCrudService {

    private final FigureRepository figureRepository;
    private final FigureMapper mapper;
    private final FigureMapper figureMapper;

    public Figure upsertFigure(FigureInfoDTO dto) {
        validateFigureInfo(dto);

        Optional<Figure> existing = figureRepository.findByFigureId(dto.figureId());

        if (existing.isPresent()) {
            log.debug("기존 국회의원 정보 업데이트: {}", dto.name());
            return updateExistingFigure(existing.get(), dto);
        } else {
            log.debug("새 국회의원 정보 생성: {}", dto.name());
            return createNewFigure(dto);
        }
    }


    // ========== Private 메서드들 ==========

    private void validateFigureInfo(FigureInfoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("국회의원 정보가 null 입니다");
        }
        if (JsonUtils.isEmpty(dto.figureId())) {
            throw new IllegalArgumentException("국회의원 ID가 필수입니다");
        }
        if (JsonUtils.isEmpty(dto.name())) {
            throw new IllegalArgumentException("국회의원 이름이 필수입니다");
        }
    }

    private Figure updateExistingFigure(Figure figure, FigureInfoDTO dto) {
        figureMapper.updateFigureFromDTO(figure, dto);
        return figureRepository.save(figure);
    }

    private Figure createNewFigure(FigureInfoDTO dto) {
        Figure figure = Figure.builder()
                .figureId(dto.figureId())
                .name(dto.name())
                .figureType(FigureType.POLITICIAN)
                .viewCount(0L)
                .build();

        figureMapper.updateFigureFromDTO(figure, dto);
        return figureRepository.save(figure);
    }
}
