package com.example.GateStatus.domain.timeline;

public enum SourceType {
    STATEMENT("STATEMENT"),
    BILL("BILL"),
    CUSTOM("CUSTOM");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }
}
