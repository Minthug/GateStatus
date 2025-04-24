package com.example.GateStatus.domain.comparison;

public enum ComparisonType {

    STATEMENT("발언"),
    VOTE("투표"),
    BILL("법안");

    private final String displayName;

    ComparisonType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
