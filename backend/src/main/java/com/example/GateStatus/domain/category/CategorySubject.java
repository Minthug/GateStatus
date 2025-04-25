package com.example.GateStatus.domain.category;

public enum CategorySubject {

    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    FOREIGN_SECURITY("외교안보"),
    OTHER("기타");

    private final String displayName;

    CategorySubject(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
