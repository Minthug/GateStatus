package com.example.GateStatus.domain.figure.service.request;

public record FindFigureCommand(Long figureId) {
    public static FindFigureCommand of(Long figureId) {
        return new FindFigureCommand(figureId);
    }
}
