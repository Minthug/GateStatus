package com.example.GateStatus.domain.timeline;

public enum TimelineEventType {
    STATEMENT("발언"),
    BILL_PROPOSED("법안 발의"),
    BILL_PASSED("법안 통과"),
    ACTIVITY("활동"),
    APPOINTMENT("임명/취임"),
    ELECTION("선거"),
    AWARD("수상"),
    SCANDAL("논란/사건"),
    OTHER("기타");

    private String displayName;

    TimelineEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
