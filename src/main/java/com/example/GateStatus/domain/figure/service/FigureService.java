package com.example.GateStatus.domain.figure.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureType;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.request.FindFigureCommand;
import com.example.GateStatus.domain.figure.service.request.RegisterFigureCommand;
import com.example.GateStatus.domain.figure.service.request.UpdateFigureCommand;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.figure.service.response.RegisterFigureResponse;
import com.example.GateStatus.domain.figure.service.response.UpdateFigureResponse;
import com.example.GateStatus.global.kubernetes.KubernetesProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FigureService {

    private final FigureRepository figureRepository;
    private final KubernetesProperties kubernetesProperties;

    @Value("${app.db-host}")
    private String dbHost;

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

    @Transactional
    public Page<FindFigureDetailResponse> findAllFigures(PageRequest pageRequest, String type, String keyword) {
        Page<Figure> figures;
        if (type != null && keyword != null) {
            figures = switch (type.toLowerCase()) {
                case "name" -> figureRepository.findByNameContaining(keyword, pageRequest);
                case "plcae" -> figureRepository.findByPlaceContaining(keyword, pageRequest);
                default -> figureRepository.findAll(pageRequest);
            };
        } else {
          figures = figureRepository.findAll(pageRequest);
        }
        return figures.map(FindFigureDetailResponse::from);
    }

    private Figure findFigureById(Long figureId) {
        return figureRepository.findById(figureId)
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));
    }

    public List<FindFigureDetailResponse> findFiguresByType(FigureType figureType) {
        return figureRepository.findByFigureType(figureType)
                .stream()
                .map(FindFigureDetailResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UpdateFigureResponse updateFigure(Long figureId, UpdateFigureCommand command) {
        Figure figure = findFigureById(figureId);
        figure.update(
                figure.getName(),
                figure.getEnglishName(),
                figure.getBirth(),
                figure.getPlace(),
                figure.getProfileUrl(),
                figure.getFigureType(),
                figure.getFigureParty(),
                figure.getEducation(),
                figure.getCareers(),
                figure.getSites(),
                figure.getActivities(),
                figure.getUpdateSource()
        );
        return UpdateFigureResponse.from(figure);
    }

    @Transactional
    public void deleteFigure(Long figureId) {
        Figure findFigure = findFigureById(figureId);
        figureRepository.delete(findFigure);
    }
}
