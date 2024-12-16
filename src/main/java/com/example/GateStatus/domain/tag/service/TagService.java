package com.example.GateStatus.domain.tag.service;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.figure.exception.NotFoundFigureException;
import com.example.GateStatus.domain.figure.repository.FigureRepository;
import com.example.GateStatus.domain.tag.Tag;
import com.example.GateStatus.domain.tag.TagRepository;
import com.example.GateStatus.domain.tag.exception.NotFoundTagException;
import com.example.GateStatus.domain.tag.service.request.AddTagCommand;
import com.example.GateStatus.domain.tag.service.request.RemoveTagCommand;
import com.example.GateStatus.domain.tag.service.response.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final FigureRepository figureRepository;

    @Transactional
    public TagResponse addTagToFigure(AddTagCommand command) {
        Figure figure = figureRepository.findById(command.figureId())
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));

        Tag tag = tagRepository.findByName(command.tagName())
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .tagName(command.tagName())
                        .build()));

        figure.addFigureTag(tag);
        return TagResponse.from(tag);
    }

    @Transactional
    public void removeTagFromFigure(RemoveTagCommand command) {
        Figure figure = figureRepository.findById(command.figureId())
                .orElseThrow(() -> new NotFoundFigureException("Figure not found"));

        Tag tag = tagRepository.findByName(command.tagName())
                .orElseThrow(() -> new NotFoundTagException("Tag not found"));

        figure.removeFigureTag(tag);
    }
}
