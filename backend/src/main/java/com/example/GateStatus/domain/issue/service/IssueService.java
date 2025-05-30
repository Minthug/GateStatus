package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.category.service.CategoryService;
import com.example.GateStatus.domain.issue.IssueCategory;
import com.example.GateStatus.domain.issue.IssueDocument;
import com.example.GateStatus.domain.issue.exception.InvalidCategoryException;
import com.example.GateStatus.domain.issue.exception.NotFoundIssueException;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.service.request.IssueRequest;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import com.example.GateStatus.global.config.EventListner.EventPublisher;
import com.example.GateStatus.global.config.EventListner.IssueLinkedToStatementEvent;
import io.kubernetes.client.extended.pager.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final CategoryService categoryService;
    private final EventPublisher eventPublisher;

    // ============================================
    // 📖 기본 조회 메서드들
    // ============================================

    /**
     * 이슈 상세 조회
     * 이슈 조회하면서 조회수 1 증가
     * @param id
     * @return
     */
    @Transactional
    public IssueResponse getIssue(String id) {
        IssueDocument issue = findByIssueById(id);
        issue.incrementViewCount();
        issueRepository.save(issue);

        log.debug("이슈 조회: ID={}, 조회수={}", id, issue.getViewCount());
        return IssueResponse.from(issue);
    }

    @Transactional
    public IssueResponse getIssueByName(String name) {
        String normalizedName = validateAndNormalizeName(name);

        IssueDocument issue = issueRepository.findByNameAndIsActiveTrue(normalizedName)
                .orElseThrow(() -> new NotFoundIssueException("이슈를 찾을 수 없습니다: " + normalizedName));

        issue.incrementViewCount();
        issueRepository.save(issue);

        log.debug("이슈 이름 조회: name={}, ID={}", normalizedName, issue.getId());
        return IssueResponse.from(issue);
    }

    /**
     * 시스템 내부용 이슈 조회 (조회수 증가 없음)
     * 연결 작업 등에서 사용
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public IssueResponse getIssuesForSystem(String id) {
        IssueDocument issue = issueRepository.findActiveIssueById(id);
        return IssueResponse.from(issue);
    }

    /**
     * 이슈 이름으로 시스템 조회 (조회수 증가 없음)
     * @param name
     * @return
     */
    @Transactional(readOnly = true)
    public IssueResponse getIssuesByNameForSystem(String name) {
        String normalizedName = validateAndNormalizeName(name);

        IssueDocument issue = issueRepository.findByNameAndIsActiveTrue(normalizedName)
                .orElseThrow(() -> new NotFoundIssueException("이슈를 찾을 수 없습니다: " + normalizedName));

        return IssueResponse.from(issue);
    }

    // ============================================
    // 🔍 검색 및 목록 조회 메서드들
    // ============================================

    /**
     * 키워드 검색
     * exact, contains, fuzzy 검색을 하나로 통합
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(String query, String searchType, Pageable pageable) {
        String normalizedQuery = validateAndNormalizeName(query);

        Page<IssueDocument> issues = switch (searchType.toLowerCase()) {
            case "exact" -> issueRepository.findByNameIgnoreCaseAndIsActiveTrue(normalizedQuery, pageable);
            case "fuzzy" -> searchWithFuzzyLogic(normalizedQuery, pageable);
            default -> issueRepository.searchByKeyword(normalizedQuery, pageable);
        };

        log.debug("이슈 검색: query={}, type={}, 결과수={}", normalizedQuery, searchType, issues.getTotalElements());
        return issues.map(IssueResponse::from);
    }

    /**
     * 카테고리별 이슈 목록 조회
     * @param categoryCode (ex: "ECONOMY", "POLITICS")
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByCategory(String categoryCode, Pageable pageable) {
        validateCategoryCode(categoryCode);

        return issueRepository.findByCategoryCodeAndIsActiveTrue(categoryCode, pageable)
                .map(IssueResponse::from);
    }


    /**
     * 인기(핫) 이슈 목록 조회
     * 우선순위와 조회수를 기준으로 정렬된 인기 이슈들을 반환
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getHotIssues(Pageable pageable) {
        return issueRepository.findByIsHotTrueAndIsActiveTrueOrderByPriorityDescViewCountDesc(pageable)
                .map(IssueResponse::from);
    }

    /**
     * 특정 정치인 관련 이슈 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByFigure(Long figureId, Pageable pageable) {
        log.debug("정치인 관련 이슈 조회: figureId={}", figureId);
        return issueRepository.findIssueByFigureId(figureId, pageable)
                .map(IssueResponse::from);
    }

    /**
     * 특정 법안 관련 이슈 목록 조회
     * @param billId
     * @return
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getIssuesByBill(String billId) {
        log.debug("법안 관련 이슈 조회: billId={}", billId);
        return issueRepository.findIssuesByBillId(billId)
                .stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 발언 관련 이슈 목록 조회
     * @param statementId
     * @return
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getIssuesByStatement(String statementId) {
        log.debug("발언 관련 이슈 조회: statementId={}", statementId);
        return issueRepository.findByRelatedStatementIdsContaining(statementId)
                .stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * 특정 태그가 포함된 이슈 목록 조회
     * @param tag
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByTag(String tag, Pageable pageable) {
        log.debug("태그별 이슈 조회: tag={}", tag);
        return issueRepository.findByTagsContainingAndIsActiveTrue(tag, pageable)
                .map(IssueResponse::from);
    }

    /**
     * 최근 이슈 목록 조회
     * 생성일시를 기준으로 내림차순 정렬하여 최신 이슈들을 반환
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getRecentIssues(Pageable pageable) {
        return issueRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(IssueResponse::from);
    }

    /**
     * 이슈 정보 업데이트
     * 요청된 필드들만 선택적으로 업데이트
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public IssueResponse updateIssue(String id, IssueRequest request) {
        log.info("이슈 업데이트 시작: id={}", id);

        IssueDocument issue = findByIssueById(id);

        issue.update(
                request.name(),
                request.description(),
                request.categoryCode(),
                request.keywords(),
                request.thumbnailUrl(),
                request.tags(),
                request.isActive(),
                request.isHot()
        );

        if (request.priority() != null) {
            issue.setPriority(request.priority());
        }

        if (request.parentIssueId() != null) {
            issue.setParentIssueId(request.parentIssueId());
        }

        issue.setUpdatedAt(LocalDateTime.now());
        IssueDocument updatedIssue = issueRepository.save(issue);

        log.info("이슈 업데이트 완료: id={}", id);
        return IssueResponse.from(updatedIssue);
    }

    /**
     * 이슈 삭제 (논리적 삭제)
     * 실제로 데이터를 삭제하지 않고 isActive를 false로 설정
     * @param id
     */
    @Transactional
    public void deleteIssue(String id) {
        log.info("이슈 논리적 삭제 시작: id={}", id);

        IssueDocument issue = findByIssueById(id);
        issue.setIsActive(false);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);

        log.info("이슈 논리적 삭제 완료: id={}", id);
    }

    /**
     * 물리적 이슈 삭제 (관리자 전용)
     * 데이터베이스에서 완전히 삭제, 복구가 불가능하므로 주의해서 사용해야 합니다
     * @param id
     */
    @Transactional
    public void hardDeleteIssue(String id) {
        log.warn("이슈 물리적 삭제 시작: id={} - 복구 불가능한 작업입니다", id);

        if (!issueRepository.existsById(id)) {
            throw new NotFoundIssueException("삭제할 이슈가 존재하지 않습니다: " + id);
        }

        issueRepository.deleteById(id);
        log.warn("이슈 물리적 삭제 완료: id={}", id);
    }


    /**
     * 관련 이슈 찾기
     * 1차: 같은 카테고리의 이슈들을 조회
     * 2차: 키워드나 태그가 유사한 이슈들을 추가 조회
     * @param issueId
     * @param limit
     * @return
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> findRelatedIssues(String issueId, int limit) {
        IssueDocument issue = findActiveIssuesById(issueId);

        // 1. 같은 카테고리 이슈 조회
        List<IssueDocument> related = issueRepository.findByRelatedIssuesByCategoryAndNotId(
                issue.getCategoryCode(), issueId, PageRequest.of(0, limit));

        // 2. 관련 이슈가 충분하지 않으면 키워드나 태그 기반으로 추가 조회
        if (related.size() < limit) {
            List<String> searchTerms = buildSearchTerms(issue);
            if (!searchTerms.isEmpty()) {
                List<IssueDocument> relatedByKeywords = issueRepository.findRelatedIssuesByKeywordsOrTags(
                        searchTerms, issueId, PageRequest.of(0, limit - related.size()));

                related.addAll(relatedByKeywords.stream()
                        .filter(relatedIssue -> related.stream()
                                .noneMatch(existing -> existing.getId().equals(relatedIssue.getId())))
                        .limit(limit - related.size())
                        .collect(Collectors.toList()));
            }
        }
        return related.stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 관련 리소스별 이슈 조회 - 통합 메서드
     * @param resourceType
     * @param resourceId
     * @return
     */
    public List<IssueResponse> getIssuesByResource(String resourceType, String resourceId) {
        List<IssueDocument> issues = switch (resourceType.toUpperCase()) {
            case "FIGURE" -> issueRepository.findIssueByFigureId(Long.parseLong(resourceId), Pageable.unpaged()).getContent();
            case "BILL" -> issueRepository.findIssuesByBillId(resourceId);
            case "STATEMENT" -> issueRepository.findByRelatedStatementIdsContaining(resourceId);
            case "NEWS" -> issueRepository.findByRelatedNewsIdsContaining(resourceId);
            default -> throw new IllegalArgumentException("지원하지 않는 리소스 타입: " + resourceType);
        };

        return issues.stream()
                .filter(IssueDocument::getIsActive)
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    private List<String> buildSearchTerms(IssueDocument issue) {
        return null;
    }

    private IssueDocument findActiveIssuesById(String issueId) {
        return null;
    }

    // ============================================
    // 🔗 리소스 연결 메서드들 (통합)
    // ============================================

    /**
     * 통합 리소스 연결 메서드
     * 모든 타입의 리소스 연결을 하나로 처리
     * @param issueId
     * @param resourceType
     * @param resourceId
     */
    @Transactional
    public void linkIssueToResource(String issueId, String resourceType, String resourceId) {
        IssueDocument issue = findActiveIssuesById(issueId);

        switch (resourceType.toUpperCase()) {
            case "BILL" -> linkToBill(issue, resourceId);
            case "STATEMENT" -> linkToStatement(issue, resourceId);
            case "FIGURE" -> linkToFigure(issue, Long.parseLong(resourceId));
            case "NEWS" -> linkToNews(issue, resourceId);
            default -> throw new IllegalArgumentException("지원허지 않는 리소스 타입: " + resourceType);
        }

        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);

        log.info("이슈 연결 완료: issueId={}, type={}, resourceId={}", issueId, resourceType, resourceId);
    }

    /**
     *  뉴스 자동 연결
     */
    @Async
    @Transactional
    public void autoLinkNewsToIssues(String newsId, String newTitle, String newContent) {
        log.info("뉴스 자동 연결 시작: newsId={}", newsId);

        String searchText = (newTitle + " " + newContent).toLowerCase();
        List<IssueDocument> candidates = issueRepository.findByIsActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, 100)).getContent();

        int linkedCount = 0;
        for (IssueDocument issue : candidates) {
            if (shouldAutoLink(issue, searchText)) {
                linkToNews(issue, newsId);
                linkedCount++;
            }
        }
        log.info("뉴스 자동 연결 완료: newsId={}, 연결수={}", newsId, linkedCount);
    }

    /**
     * 내부용 ID 찾기
     * 다른 메서드들에서 공통으로 사용하는 이슈 조회 로직
     * @param id
     * @return
     */
    private IssueDocument findByIssueById(String id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다" + id));
    }


    /**
     * 뉴스와 이슈 연결
     * @param issueId
     * @param newsId
     */
    @Transactional
    public void linkNewsToIssue(String issueId, String newsId) {
        log.info("이슈-뉴스 연결 시작: issueId={}, newsId={}", issueId, newsId);

        IssueDocument issue = findByIssueById(issueId);

        if (issue.getRelatedNewsIds() == null) {
            issue.setRelatedNewsIds(new ArrayList<>());
        }

        if (!issue.getRelatedNewsIds().contains(newsId)) {
            issue.getRelatedNewsIds().add(newsId);
            issue.setUpdatedAt(LocalDateTime.now());
            issueRepository.save(issue);
            log.info("이슈-뉴스 연결 완료: issueId={}, newsId={}", issueId, newsId);

        } else {
            log.debug("이미 연결된 이슈-뉴스: issueId={}, newsId={}", issueId, newsId);
        }
    }

    /**
     * 뉴스와 이슈 연결 해제
     */
    @Transactional
    public void unlinkNewsFromIssue(String issueId, String newsId) {
        log.info("이슈-뉴스 연결 해제 시작: issueId={}, newsId={}", issueId, newsId);

        IssueDocument issue = findByIssueById(issueId);

        if (issue.getRelatedNewsIds() != null && issue.getRelatedNewsIds().contains(newsId)) {
            issue.getRelatedNewsIds().remove(newsId);
            issue.setUpdatedAt(LocalDateTime.now());
            issueRepository.save(issue);

            log.info("이슈-뉴스 연결 해제 완료: issueId={}, newsId={}", issueId, newsId);
        }
    }

    /**
     * 특정 뉴스와 연결된 이슈 목록 조회
     * @param newsId
     * @return
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getIssuesByNews(String newsId) {
        log.debug("뉴스 관련 이슈 조회: newsId={}", newsId);

        List<IssueDocument> issues = issueRepository.findByRelatedNewsIdsContaining(newsId);

        return issues.stream()
                .filter(issue -> issue.getIsActive())
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    @Async
    @Transactional
    public void autoLinkNewsToIssues(String newsId, String newsTitle, String newsContent) {
        log.info("뉴스 자동 연결 시작: newsId={}", newsId);

        String searchText = (newsTitle + " " + newsContent).toLowerCase();
        List<IssueDocument> allActiveIssues = issueRepository.findByIsActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, 100)).getContent();

        int linkedCount = 0;
        for (IssueDocument issue : allActiveIssues) {
            if (shouldLinkNewsToIssue(issue, searchText)) {
                linkNewsToIssue(issue.getId(), newsId);
                linkedCount++;
            }
        }
        log.info("뉴스 자동 연결 완료: newsId={}, 연결된 이슈 수={}", newsId, linkedCount);
    }

    /**
     * 뉴스와 이슈 연결 여부 판단 (내부용)
     * 키워드와 태그를 기반으로 연관성 판단
     */
    private boolean shouldLinkNewsToIssue(IssueDocument issue, String newsText) {
        if (issue.getKeywords() != null) {
            for (String keyword : issue.getKeywords()) {
                if (newsText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        if (issue.getTags() != null) {
            for (String tag : issue.getTags()) {
                if (newsText.contains(tag.toLowerCase())) {
                    return true;
                }
            }
        }

        if (issue.getName() != null && newsText.contains(issue.getName().toLowerCase())) {
            return true;
        }

        return false;
    }


    // ============================================
    // 🔗 개별 리소스 연결 메서드들 (private)
    // ============================================



    // ============================================
    // 🔧 내부 유틸리티 메서드들
    // ============================================
    private String validateAndNormalizeName(String name) {
        return null;
    }


    private Page<IssueDocument> searchWithFuzzyLogic(String normalizedQuery, Pageable pageable) {
        return null;
    }

    /**
     * 카테고리 코드 검증
     * @param categoryCode
     */
    private void validateCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new InvalidCategoryException("카테고리 코드가 필요합니다");
        }

        try {
            IssueCategory.fromCode(categoryCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidCategoryException("유효하지 않은 카테고리 코드: " + categoryCode);
        }
    }

}
