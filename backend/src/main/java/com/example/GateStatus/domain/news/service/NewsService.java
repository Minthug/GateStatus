package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsDocument saveNews(NewsDocument news) {

        if (news.getContentHash() != null) {
            Optional<NewsDocument> existing = newsRepository.findByContentHash(news.getContentHash());
            if (existing.isPresent()) {
                log.warn("중복 뉴스 감지: {}", news.getTitle());
                return existing.get();
            }
        }

        if (news.getCreatedAt() == null) {
            news.setCreatedAt(LocalDateTime.now());
        }

        NewsDocument saved = newsRepository.save(news);
        log.info("뉴스 저장 완료: id={}, title={}", saved.getId(), saved.getTitle());

        return saved;
    }

    @Transactional(readOnly = true)
    public NewsDocument getNews(String newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new EntityNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));
    }

    @Transactional(readOnly = true)
    public Page<NewsDocument> getNewsByCategory(String category, Pageable pageable) {
        log.debug("카테고리별 뉴스 조회: category={}", category);

        if (category == null || "ALL".equalsIgnoreCase(category)) {
            return newsRepository.findAllByOrderByPubDateDesc(pageable);
        }

        return newsRepository.findByCategoryOrderByPubDateDesc(category, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NewsDocument> getRecentNews(Pageable pageable) {
        return newsRepository.findAllByOrderByPubDateDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<NewsDocument> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }
        return newsRepository.findByPubDateBetween(startDate, endDate, pageable);
    }

    @Transactional
    public void markAsProcessed(String newsId) {
        NewsDocument news = getNews(newsId);

        news.setProcessed(true);
        news.setProcessedAt(LocalDateTime.now());
        newsRepository.save(news);

        log.info("뉴스 처리 완료 표시: id={}", newsId);
    }

    /**
     * 관리자가 수동으로 뉴스를 이슈에 연결
     * @param newsId
     * @param issueId
     */
    @Transactional
    public void linkNewsToIssue(String newsId, String issueId) {

        NewsDocument news = getNews(newsId);

        if (news.getRelatedIssueId() != null && !news.getRelatedIssueId().equals(issueId)) {
            log.warn("뉴스가 이미 다른 이슈와 연결됨: newsId={}, existingIssueId={}, newIssueId={}",
                    newsId, news.getRelatedIssueId(), issueId);
        }

        news.setRelatedIssueId(issueId);
        news.setProcessed(true);
        news.setProcessedAt(LocalDateTime.now());
        newsRepository.save(news);

        log.info("뉴스-이슈 연결 완료: newsId={}, issueId={}", newsId, issueId);
    }

    /**
     * 이슈와 연결되지 않은 뉴스 목록 조회 (관리자용)
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<NewsDocument> getUnlinkedNews(Pageable pageable) {
        return newsRepository.findByRelatedIssueIdIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Page<NewsDocument> getNewsByIssue(String issueId, Pageable pageable) {
        return newsRepository.findByRelatedIssueIdOrderByPubDateDesc(issueId, pageable);
    }
}
