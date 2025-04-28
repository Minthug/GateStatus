package com.example.GateStatus.domain.figure.service.request;

public record FindFigureCommand(String figureId) {
    public static FindFigureCommand of(String figureId) {
        return new FindFigureCommand(figureId);
    }
}
