//package com.example.GateStatus.global.config;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Configuration
//public class NewsConfig {
//
//    @Value("${news.api.naver.base-url}")
//    private String naverBaseUrl;
//
//
//    @Bean
//    public WebClient naverWebClient(WebClient.Builder builder) {
//        return builder
//                .baseUrl(naverBaseUrl)
//                .codecs(configurer -> configurer
//                        .defaultCodecs()
//                        .maxInMemorySize(10 * 1024 * 1024)) // 10 MB
//                .build();
//    }
//}
