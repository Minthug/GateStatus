package com.example.GateStatus.domain.tag.service.response;

import com.example.GateStatus.domain.figure.FigureTag;
import com.example.GateStatus.domain.tag.Tag;

public record TagResponse(Long tagId, String tagName) {
    public static TagResponse from(FigureTag figureTag) {
        return new TagResponse(
                figureTag.getTag().getId(),
                figureTag.getTag().getTagName());
    }
}
