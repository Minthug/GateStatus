package com.example.GateStatus.domain.tag.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.FigureTag;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.figure.service.response.FindFigureDetailResponse;
import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.domain.tag.TagRepository;
import com.example.GateStatus.domain.tag.exception.NotFoundTagException;
import com.example.GateStatus.domain.tag.service.request.AddTagCommand;
import com.example.GateStatus.domain.tag.service.request.RemoveTagCommand;
import com.example.GateStatus.domain.tag.service.response.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final FigureRepository figureRepository;

    @Transactional
    public TagResponse addTagToFigure(AddTagCommand command) {
        Figure figure = figureRepository.findById(command.figureId())
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));

        Tag tag = tagRepository.findByTagName(command.tagName())
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .tagName(command.tagName())
                        .build()));

        FigureTag figureTag = figure.addFigureTag(tag);
        return TagResponse.from(figureTag);
    }

    @Transactional
    public void removeTagFromFigure(RemoveTagCommand command) {
        Figure figure = figureRepository.findById(command.figureId())
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));

        Tag tag = tagRepository.findByTagName(command.tagName())
                .orElseThrow(() -> new NotFoundTagException("Tag not found"));

        figure.removeFigureTag(tag);
    }

    public List<TagResponse> getFigureTags(Long figureId) {
        return figureRepository.findById(figureId)
                .orElseThrow(() -> new NotFoundTagException("Figure not found"))
                .getFigureTag().stream()
                .map(TagResponse::from)
                .collect(Collectors.toList());
    }

    public List<FindFigureDetailResponse> getFigureByTags(String tagName) {
        Tag tag = tagRepository.findByTagName(tagName)
                .orElseThrow(() -> new NotFoundTagException("Tag not found"));

        return tag.getFigureTags().stream()
                .map(FigureTag::getFigure)
                .map(FindFigureDetailResponse::from)
                .collect(Collectors.toList());
    }
}
