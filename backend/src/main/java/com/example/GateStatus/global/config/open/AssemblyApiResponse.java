package com.example.GateStatus.global.config.open;

public record AssemblyApiResponse<T>(String resultCode, String resultMessage, T data) {

    public T getData() {
        return data;
    }


    public boolean isSuccess() {
        return "00".equals(resultCode) || "INFO-000".equals(resultCode);
    }
}
