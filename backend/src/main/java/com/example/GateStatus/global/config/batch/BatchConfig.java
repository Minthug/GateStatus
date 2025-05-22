package com.example.GateStatus.global.config.batch;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Map;

@Configuration
@EnableJpaRepositories
public class BatchConfig {

    @Bean
    @Primary
    public EntityManagerFactory batchEntityManagerFactory() {
        return Persistence.createEntityManagerFactory("batchUnit", Map.of(
                "hibernate.jdbc.batch_size", "50",
                "hibernate.order_inserts", "true",
                "hibernate.order_updates", "true",
                "hibernate.batch_versioned_data", "true"
        ));
    }


}
