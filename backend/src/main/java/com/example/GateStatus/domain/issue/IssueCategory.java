package com.example.GateStatus.domain.issue;

public enum IssueCategory {

    ECONOMY("ECONOMY", "경제"),
    FOREIGN_POLICY("FOREIGN_POLICY", "외교"),
    POLITICS("POLITICS", "정치"),
    SOCIAL("SOCIAL", "사회"),
    EDUCATION("EDUCATION", "교육"),
    ENVIRONMENT("ENVIRONMENT", "환경"),
    HEALTH("HEALTH", "보건/의료"),
    DEFENSE("DEFENSE", "국방"),
    CULTURE("CULTURE", "문화"),
    TECHNOLOGY("TECHNOLOGY", "과학기술"),
    WELFARE("WELFARE", "복지"),
    LABOR("LABOR", "노동"),
    HOUSING("HOUSING", "주택"),
    TRANSPORT("TRANSPORT", "교통"),
    SECURITY("SECURITY", "안보"),
    ADMIN("ADMIN", "행정"),
    JUSTICE("JUSTICE", "법무"),
    OTHER("OTHER", "기타");


    private final String code;
    private final String displayName;

    IssueCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static IssueCategory fromCode(String code) {
        for (IssueCategory category : IssueCategory.values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }

        return OTHER;
    }
}
