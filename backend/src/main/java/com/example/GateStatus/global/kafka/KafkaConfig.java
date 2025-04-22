//package com.example.GateStatus.global.kafka;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.apache.kafka.clients.admin.AdminClientConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaAdmin;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaConfig {
//
//    private String bootStrapServer;
//
//    @Bean
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//    }
//
//    @Bean
//    public KafkaAdmin kafkaAdmin() {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
//        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
//        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
//        configs.put(AdminClientConfig.RETRIES_CONFIG, 3);
//        configs.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, 10000);
//        return new KafkaAdmin(configs);
//    }
//
//    @Bean
//    public ProducerFactory<String, Object> producerFactory() {
//        Map<String, Object> config = new HashMap<>();
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//
//        config.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 복제본이 메시지를 받았는지 확인
//        config.put(ProducerConfig.RETRIES_CONFIG, 3);   // 실패 시 재시도 횟수
//        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 배치 크기 설정
//        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);  // 배치 전송 대기 시간
//
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//
//    @Bean
//    public KafkaTemplate<String, Object> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
////    @Bean(name = "KafkaListenerContainerFactory")
////    public ConcurrentKafkaListenerContainerFactory<String, ErrandStatusMessage> kafkaListenerContainerFactory() {
////        ConcurrentKafkaListenerContainerFactory<String, ErrandStatusMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
////        factory.setConsumerFactory(consumerFactory());
////
////        // 에러 처리 및 재시도 정책 설정
////        factory.setCommonErrorHandler(new DefaultErrorHandler(
////                new DeadLetterPublishingRecoverer(kafkaTemplate()),
////                new FixedBackOff(1000L, 3L)  // 1초 간격으로 3번 재시도
////        ));
////
////        // 배치 리스너 설정
////        factory.setBatchListener(true);
////
////        return factory;
////    }
////
////    @Bean
////    public NewTopic errandTopic() {
////        return TopicBuilder.name("errand-status-topic")
////                .partitions(3)
////                .replicas(1)
////                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(24 * 60 * 60 * 1000)) // 24시간 보관
////                .build();
////    }
////
//
//
//}
