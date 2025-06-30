package com.example.GateStatus.domain.news.service;

import com.example.GateStatus.domain.news.NewsDocument;
import com.example.GateStatus.domain.news.dto.NewsUpdateRequest;
import com.example.GateStatus.domain.news.repository.NewsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        if (news.getProcessed() == null) {
            news.setProcessed(false);
        }

        if (news.getExtractedKeywords() == null) {
            news.setExtractedKeywords(new ArrayList<>());
        }

        if (news.getMentionedFigureIds() == null) {
            news.setMentionedFigureIds(new ArrayList<>());
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
        return newsRepository.findAllByPubDateBetween(startDate, endDate, pageable);
    }

    @Transactional
    public void markAsProcessed(String newsId) {
        NewsDocument news = getNews(newsId);

        if (Boolean.TRUE.equals(news.getProcessed())) {
            log.warn("이미 처리된 뉴스입니다: {}", newsId);
            return;
        }
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

    public void unlinkFromIssue(String newsId) {
        NewsDocument news = getNews(newsId);

        if (news.getRelatedIssueId() == null) {
            log.warn("이미 이슈와 연결되지 않은 뉴스입니다: {}", newsId);
            return;
        }

        String previousIssueId = news.getRelatedIssueId();
        news.setRelatedIssueId(null);
        news.setProcessed(false); // 다시 처리 대상으로 변경
        newsRepository.save(news);

        log.info("뉴스-이슈 연결 해제: newsId={}, previousIssueId={}", newsId, previousIssueId);
    }

    /**
     * 이슈와 연결되지 않은 뉴스 목록 조회 (관리자용)
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<NewsDocument> getUnlinkedNews(Pageable pageable) {
        return newsRepository.findByRelatedIssueIdIsNullOrderByPubDateDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<NewsDocument> getNewsByIssue(String issueId, Pageable pageable) {
        return newsRepository.findByRelatedIssueIdOrderByPubDateDesc(issueId, pageable);
    }

    /**
     * 처리되지 않은 뉴스 목록 조회
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<NewsDocument> getUnProcessedNews(Pageable pageable) {
        return newsRepository.findByProcessedFalseOrderByPubDateDesc(pageable);
    }

    /**
     * 키워드로 뉴스 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<NewsDocument> searchNews(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수 입니다");
        }

        return newsRepository.searchByKeyword(keyword.trim(), pageable);
    }

    /**
     * 특정 정치인이 언급된 뉴스 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<NewsDocument> getNewsByFigure(Long figureId, Pageable pageable) {
        return newsRepository.findByMentionedFigure(figureId, pageable);
    }

    /**
     * 뉴스 수정
     * @param newsId
     * @param updateRequest
     * @return
     */
    @Transactional
    public NewsDocument updateNews(String newsId, NewsUpdateRequest updateRequest) {
        NewsDocument news = getNews(newsId);

        if (updateRequest.category() != null) {
            news.setCategory(updateRequest.category());
        }
        if (updateRequest.extractedKeywords() != null) {
            news.setExtractedKeywords(updateRequest.extractedKeywords());
        }
        if (updateRequest.mentionedFigureIds() != null) {
            news.setMentionedFigureIds(updateRequest.mentionedFigureIds());
        }

        news.setUpdatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    /**
     * 뉴스 삭제 (물리적 삭제)
     * @param newsId
     */
    @Transactional
    public void deleteNews(String newsId) {
        if (!newsRepository.existsById(newsId)) {
            throw new EntityNotFoundException("삭제할 뉴스가 존재하지 않습니다: " + newsId);
        }

        newsRepository.deleteById(newsId);
        log.info("뉴스 삭제 완료: id={}", newsId);
    }

    /**
     * 뉴스 일괄 처리 (배치 작업용)
     * @param newsIds
     * @param processed
     */
    @Transactional
    public void batchUpdateProcessedStatus(List<String> newsIds, boolean processed) {
        List<NewsDocument> newsList = newsRepository.findAllById(newsIds);

        LocalDateTime now = LocalDateTime.now();
        newsList.forEach(news -> {
            news.setProcessed(processed);
            if (processed) {
                news.setProcessedAt(now);
            }
        });

        newsRepository.saveAll(newsList);
        log.info("뉴스 일괄 처리 완료: {}건, processed={}", newsList.size(), processed);
    }

    /**
     * 오래된 뉴스 정리
     * @param daysToKeep
     * @return
     */
    @Transactional
    public long cleanupOldNews(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        long deletedCount = newsRepository.deleteByCreatedAtBeforeAndRelatedIssueIdIsNull(cutoffDate);
        log.info("오래된 뉴스 정리 완료: {}일 이전 뉴스 {}건 삭제", daysToKeep, deletedCount);

        return deletedCount;
    }

}
