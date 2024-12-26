package com.example.GateStatus.domain.figure;

import lombok.Getter;

@Getter
public enum FigureType {

    POLITICIAN("정치인"),
    BUSINESSMAN("기업인");

    private final String displayName;

    FigureType(String displayName) {
        this.displayName = displayName;
    }
}
