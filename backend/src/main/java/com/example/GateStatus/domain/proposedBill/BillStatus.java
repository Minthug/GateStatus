package com.example.GateStatus.domain.proposedBill;

public enum BillStatus {

    PROPOSED("발의"),
    IN_COMMITTEE("위원회 심사중"),
    IN_PLENARY("본회의 상정"),
    PASSED("가결"),
    REJECTED("부결"),
    WITHDRAWN("철회"),
    EXPIRED("임기만료 폐기");

    private final String displayName;

    BillStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
