package com.example.GateStatus.domain.statement.service;

public enum SearchType {
    FULL_TEXT,         // 전체 텍스트 검색 (제목 + 내용)
    EXACT_PHRASE,      // 정확한 문구 검색
    MULTIPLE_KEYWORDS, // 다중 키워드 검색 (AND 조건)
    CONTENT_ONLY,      // 내용만 검색
    RECENT             // 최근 발언 검색
}
