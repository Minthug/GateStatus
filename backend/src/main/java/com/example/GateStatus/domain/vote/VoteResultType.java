package com.example.GateStatus.domain.vote;

public enum VoteResultType {

    AGREE("찬성"),
    DISAGREE("반대"),
    ABSTAIN("기권"),
    ABSENT("불참"),
    UNKNOWN("알 수 없음");

    private final String displayName;

    VoteResultType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VoteResultType fromString(String value) {
        if (value == null) return UNKNOWN;

        return switch (value.trim()) {
            case "찬성" -> AGREE;
            case "반대" -> DISAGREE;
            case "기권" -> ABSTAIN;
            case "불참" -> ABSTAIN;
            default -> UNKNOWN;
        };
    }
}
