package com.example.GateStatus.global.config.open;

public record ApiResponse<T>(String status, String message, T body) {

    public boolean isSuccess() {
        return "OK".equals(status);
    }
}
