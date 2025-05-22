package com.example.GateStatus.global.config.batch;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

//@Configuration
//public class BatchConfig {
//
//    @Bean("batchEntityManagerFactory")
//    public EntityManagerFactory batchEntityManagerFactory() {
//        return Persistence.createEntityManagerFactory("batchUnit", Map.of(
//                "hibernate.jdbc.batch_size", "50",
//                "hibernate.order_inserts", "true",
//                "hibernate.order_updates", "true",
//                "hibernate.batch_versioned_data", "true"
//        ));
//    }
//
//
//}
