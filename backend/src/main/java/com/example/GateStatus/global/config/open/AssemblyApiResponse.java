package com.example.GateStatus.global.config.open;

public record AssemblyApiResponse<T>(String resultCode, String resultMessage, T data, int totalCount) {

    public T getData() {
        return data;
    }
}
