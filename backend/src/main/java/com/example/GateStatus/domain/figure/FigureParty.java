package com.example.GateStatus.domain.figure;

import lombok.Getter;

@Getter
public enum FigureParty {

    DEMOCRATIC("더불어민주당", "진보"),
    PEOPLE_POWER("국민의힘", "보수"),
    JUSTICE("정의당", "진보"),
    PEOPLES("국민의당", "중도"),
    BASIC_INCOME("기본소득당", "진보"),
    TIME_TRANSITION("시대전환", "진보"),
    INDEPENDENT("무소속", "무소속"),
    LEFT("진보", "진보"),
    RIGHT("보수", "보수"),
    OTHER("기타", "기타");

    private final String partyName; // 정당
    private final String orientation; // 성향

    FigureParty(String partyName, String orientation) {
        this.partyName = partyName;
        this.orientation = orientation;
    }
}
