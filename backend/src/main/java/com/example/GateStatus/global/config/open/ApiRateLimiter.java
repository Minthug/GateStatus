package com.example.GateStatus.global.config.open;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

@Component
public class ApiRateLimiter {

    private final RateLimiter rateLimiter = RateLimiter.create(10);

    public void acquire() {
        rateLimiter.acquire();
    }
}
