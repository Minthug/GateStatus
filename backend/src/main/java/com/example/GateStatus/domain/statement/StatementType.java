package com.example.GateStatus.domain.statement;

public enum StatementType {
    SPEECH("연설문"),
    INTERVIEW("인터뷰"),
    PRESS_RELEASE("보도자료"),
    DEBATE("토론회"),
    ASSEMBLY_SPEECH("국회 연설"),
    COMMITTEE_SPEECH("상임위 발언"),
    MEDIA_COMMENT("언론 논평"),
    SOCIAL_MEDIA("SNS 발언"),
    OTHER("기타");

    private final String displayName;

    StatementType(String displayName) {
        this.displayName = displayName;
    }
}
