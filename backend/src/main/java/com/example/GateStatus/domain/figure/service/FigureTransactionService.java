package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FigureInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureTransactionService {

    private final FigureRepository figureRepository;
    private final FigureMapper figureMapper;

    /**
     * 단일 국회의원 정보 저장 또는 업데이트 (별도 트랜잭션)
     * @param info
     * @return
     */
    @Transactional
    public boolean saveOrUpdateFigure(FigureInfoDTO info) {
        try {
            Figure figure = figureRepository.findByFigureId(info.figureId())
                    .orElseGet(() -> Figure.builder()
                            .figureId(info.figureId())
                            .name(info.name())
                            .figureType(FigureType.POLITICIAN)
                            .viewCount(0L)
                            .build());

            figureMapper.updateFigureFromDTO(figure, info);
            figureRepository.saveAndFlush(figure);

            log.info("국회의원 저장 성공: {}", info.name());
            return true;
        } catch (Exception e) {
            log.error("국회의원 저장 실패: {} - {}",info.name(), e.getMessage(), e);
            return false;
        }
    }
}
