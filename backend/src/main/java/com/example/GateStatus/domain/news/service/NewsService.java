package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsDocument saveNews(NewsDocument news) {
        return newsRepository.save(news);
    }

    public Page<NewsDocument> getNewsByCategory(String category, Pageable pageable) {

    }

    public void markAsProcessed(String newsId) {

    }

    /**
     * 관리자가 수동으로 뉴스를 이슈에 연결
     * @param newsId
     * @param issueId
     */
    public void linkNewsToIssue(String newsId, String issueId) {
        NewsDocument news = newsRepository.findById(newsId)
                .orElseThrow(() -> new EntityNotFoundException("뉴스를 찾을 수 없습니다"));

        news.setRelatedIssueId(issueId);
        news.setProcessed(true);
        newsRepository.save(news);
    }

    /**
     * 이슈와 연결되지 않은 뉴스 목록 조회 (관리자용)
     * @param pageable
     * @return
     */
    public Page<NewsDocument> getUnlinkedNews(Pageable pageable) {
        return newsRepository.findByRelatedIssueIdIsNull(pageable);
    }
}
