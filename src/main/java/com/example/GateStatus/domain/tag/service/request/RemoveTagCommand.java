package com.example.GateStatus.domain.tag.service.request;

public record RemoveTagCommand(Long figureId, String tagName) {
    public static RemoveTagCommand of(final Long figureId, final String tagName) {
        return new RemoveTagCommand(figureId, tagName);
    }
}
