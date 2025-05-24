package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsApiService {

    private final WebClient webClient;
    private final NewsRepository newsRepository;


    private String naverClientId;

    private String naverClientSecret;
}
