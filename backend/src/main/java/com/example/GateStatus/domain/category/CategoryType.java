package com.example.GateStatus.domain.category;

public enum CategoryType {

    FIGURE("정치인"),
    STATEMENT("발언"),
    ISSUE("이슈");

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
