package com.example.GateStatus.domain.news;

public enum NewsSource {
    NAVER("네이버"),
    DAUM("다음"),
    GOOGLE("구글"),
    OTHER("기타");

    private final String displayName;

    NewsSource(String displayName) {
        this.displayName = displayName;
    }
}
