package com.example.GateStatus.global.config.open;

public interface ApiMapper<T, R> {

    R map(AssemblyApiResponse<T> response);
}
