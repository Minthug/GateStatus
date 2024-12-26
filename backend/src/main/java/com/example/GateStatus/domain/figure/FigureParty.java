package com.example.GateStatus.domain.figure;

import lombok.Getter;

@Getter
public enum FigureParty {

    LEFT("진보"),
    RIGHT("보수");

    private final String partyName;

    FigureParty(String partyName) {
        this.partyName = partyName;
    }
}
