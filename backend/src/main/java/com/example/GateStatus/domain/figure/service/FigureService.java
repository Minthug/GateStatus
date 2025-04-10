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
    private final FigureApiService figureApiService;
    private final FigureCacheService figureCacheService;


    @Transactional
    public RegisterFigureResponse getRegisterFigure(final RegisterFigureCommand command) {

        Figure findFigure = figureRepository.findByName(command.name())
                .orElseGet(() -> {
                    Figure figure = Figure.builder()
                            .name(command.name())
                            .englishName(command.englishName())
                            .birth(command.birth())
                            .constituency(command.constituency())
                            .profileUrl(command.profileUrl())
                            .figureType(command.figureType())
                            .education(command.education())
                            .careers(command.careers())
                            .sites(command.sites())
                            .activities(command.activities())
                            .updateSource(command.updateSource())
                            .build();
                    Figure savedFigure = figureRepository.save(figure);

                    figureCacheService.updateFigureCache(savedFigure);

                    return savedFigure;
                });
        return RegisterFigureResponse.from(findFigure);
    }

    @Transactional(readOnly = true)
    public FindFigureDetailResponse findFigure(FindFigureCommand command) {

        Figure findFigure = figureCacheService.findFigureById(command.figureId());

        figureCacheService.incrementViewCount(command.figureId());

        return FindFigureDetailResponse.from(findFigure);
    }

    @Transactional(readOnly = true)
    public Page<FindFigureDetailResponse> findAllFigures(PageRequest pageRequest, String type, String keyword) {
        Page<Figure> figures;
        if (type != null && keyword != null) {
            figures = switch (type.toLowerCase()) {
                case "name" -> figureRepository.findByNameContaining(keyword, pageRequest);
                case "place" -> figureRepository.findByPlaceContaining(keyword, pageRequest);
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

    @Transactional(readOnly = true)
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
                command.name(),
                command.englishName(),
                command.birth(),
                command.place(),
                command.profileUrl(),
                command.figureType(),
                command.figureParty(),
                command.education(),
                command.careers(),
                command.sites(),
                command.activities(),
                command.updateSource()
        );

        figureCacheService.updateFigureCache(figure);

        return UpdateFigureResponse.from(figure);
    }

    @Transactional
    public void deleteFigure(Long figureId) {
        Figure findFigure = findFigureById(figureId);
        figureRepository.delete(findFigure);

        figureCacheService.evictFigureCache(figureId);
    }

    @Transactional(readOnly = true)
    public List<FindFigureDetailResponse> getPopularFigures(int limit) {
        return figureCacheService.getPopularFigures(limit).stream()
                .map(FindFigureDetailResponse::from)
                .collect(Collectors.toList());
    }

    public Figure syncFromApi(String name) {
        return figureApiService.syncFigureInfoByName(name);
    }

    public FindFigureDetailResponse findFigureWithCache(Long id) {
        Figure figure = figureCacheService.findFigureById(id);
        return FindFigureDetailResponse.from(figure);
    }
}
