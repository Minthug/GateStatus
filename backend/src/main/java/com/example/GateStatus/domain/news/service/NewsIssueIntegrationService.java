package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsIssueIntegrationService {

    private final NewsRepository newsRepository;
}
