package com.example.GateStatus.domain.tag.service.request;

public record AddTagCommand(Long figureId, String tagName) {
    public static AddTagCommand of(final Long figureId, final String tagName) {
        return new AddTagCommand(figureId, tagName);
    }
}
