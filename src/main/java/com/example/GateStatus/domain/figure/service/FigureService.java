package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.request.FindFigureCommand;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureRepository figureRepository;

    @Transactional
    public RegisterFigureResponse getRegisterFigure(final RegisterFigureCommand command) {
        Figure findFigure = figureRepository.findByName(command.name())
                .orElseGet(() -> {
                    Figure figure = Figure.builder()
                            .name(command.name())
                            .englishName(command.englishName())
                            .birth(command.birth())
                            .place(command.place())
                            .profileUrl(command.profileUrl())
                            .figureType(command.figureType())
                            .education(command.education())
                            .careers(command.careers())
                            .sites(command.sites())
                            .activities(command.activities())
                            .updateSource(command.updateSource())
                            .build();
                    figureRepository.save(figure);
                    return figure;
                });
        return RegisterFigureResponse.from(findFigure);
    }

    @Transactional
    public FindFigureDetailResponse findFigure(FindFigureCommand command) {
        Figure findFigure = findFigureById(command.figureId());
        return FindFigureDetailResponse.from(findFigure);
    }

    private Figure findFigureById(Long figureId) {
        return figureRepository.findById(figureId)
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));
    }

    @Transactional
    public void deleteFigure(Long figureId) {
        Figure findFigure = findFigureById(figureId);
        figureRepository.delete(findFigure);
    }
}
