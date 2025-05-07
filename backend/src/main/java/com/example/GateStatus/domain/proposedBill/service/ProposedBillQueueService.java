package com.example.GateStatus.domain.proposedBill.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProposedBillQueueService {

    private final RedisTemplate redisTemplate;



    public void queueBillsSyncTask(String proposerName) {
        redisTemplate.convertAndSend("bill-sync-exchange", "bill.sync", proposerName);
    }

    @Data
    public static class JobStatus {

    }
}
