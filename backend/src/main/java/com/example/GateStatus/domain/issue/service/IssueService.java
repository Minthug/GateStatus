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
        log.debug("이슈 조회 및 조회수 증가: ID={}, 현재 조회수={}", id, issue.getViewCount());
        return IssueResponse.from(issue);
    }

    /**
     * 활성화된 이슈만 찾기 ( 조회수 증가 없음)
     * 관리자나 시스템에서 이슈 정보만 확인할 때 사용
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public IssueResponse getActiveIssue(String id) {
        IssueDocument issue = issueRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다" + id));

        return IssueResponse.from(issue);
    }

    /**
     * 카테고리별 이슈 목록 조회
     * @param categoryCode (ex: "ECONOMY", "POLITICS")
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByCategory(String categoryCode, Pageable pageable) {
        log.debug("카테고리별 이슈 조회: categoryCode={}", categoryCode);
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
     * 키워드 검색
     * MongoDB의 full-text search를 활용하여 제목, 설명, 키워드에서 검색합니다.
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(String keyword, Pageable pageable) {
        log.debug("이슈 키워드 검색: keyword={}", keyword);
        return issueRepository.searchByKeyword(keyword, pageable)
                .map(IssueResponse::from);
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
     * 새 이슈 생성
     * 카테고리 유효성 검증 후 이슈를 생성하고 저장
     * @param request
     * @return
     */
    @Transactional
    public IssueResponse createIssue(IssueRequest request) {

        log.info("새 이슈 생성 시작: name={}, categoryCode={}", request.name(), request.categoryCode());

        IssueCategory category = validateCategory(request.categoryCode());

        IssueDocument document = IssueDocument.builder()
                .name(request.name())
                .description(request.description())
                .categoryCode(category.getCode())
                .categoryName(category.getDisplayName())
                .keywords(request.keywords())
                .thumbnailUrl(request.thumbnailUrl())
                .parentIssueId(request.parentIssueId())
                .isActive(true)
                .priority(request.priority())
                .isHot(request.isHot() != null ? request.isHot() : false)
                .tags(request.tags())
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        IssueDocument savedIssue = issueRepository.save(document);
        log.info("새 이슈 생성 완료: id={}, name={}", savedIssue.getId(), savedIssue.getName());
        return IssueResponse.from(savedIssue);
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
    public List<IssueResponse> findRelatedIssue(String issueId, int limit) {
        log.debug("관련 이슈 검색 시작: issueId={}, limit={}", issueId, limit);

        IssueDocument issue = findByIssueById(issueId);

        // 1. 같은 카테고리 이슈 조회
        List<IssueDocument> relatedByCategory = issueRepository.findRelatedIssuesByCategoryAndNotId(
                issue.getCategoryCode(), issueId, PageRequest.of(0, limit));

        // 2. 관련 이슈가 충분하지 않으면 키워드나 태그 기반으로 추가 조회
        if (relatedByCategory.size() < limit) {
            List<String> searchTerms = new ArrayList<>();
            if (issue.getKeywords() != null) {
                searchTerms.addAll(issue.getKeywords());
            }
            if (issue.getTags() != null) {
                searchTerms.addAll(issue.getTags());
            }

            if (!searchTerms.isEmpty()) {
                List<IssueDocument> relatedByKeywords = issueRepository.findRelatedIssuesByKeywordsOrTags(
                        searchTerms, issueId, PageRequest.of(0, limit - relatedByCategory.size()));

                // 중복 제거하며 추가
                for (IssueDocument relatedIssue : relatedByKeywords) {
                    if (relatedByCategory.stream().noneMatch(i -> i.getId().equals(relatedIssue.getId()))) {
                        relatedByCategory.add(relatedIssue);
                        if (relatedByCategory.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }

        log.debug("관련 이슈 검색 완료: issueId={}, 찾은 개수={}", issueId, relatedByCategory.size());
        return relatedByCategory.stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 법안에 관련된 이슈 연결
     * 이슈의 relatedBillIds 목록에 법안 ID를 추가
     * @param issueId
     * @param billId
     */
    @Transactional
    public void linkIssuesToBill(String issueId, String billId) {
        log.info("이슈-법안 연결 시작: issueId={}, billId={}", issueId, billId);

        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedBill(billId);
        issueRepository.save(issue);

        log.info("이슈-법안 연결 완료: issueId={}, billId={}", issueId, billId);
    }

    /**
     * 특정 정치인에 관련된 이슈 연결
     * 이슈의 relatedFigureIds 목록에 정치인 ID를 추가
     * @param issueId
     * @param figureId
     */
    @Transactional
    public void linkIssuesToFigure(String issueId, Long figureId) {
        log.info("이슈-정치인 연결 시작: issueId={}, figureId={}", issueId, figureId);

        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedFigure(figureId);
        issueRepository.save(issue);

        log.info("이슈-정치인 연결 완료: issueId={}, figureId={}", issueId, figureId);
    }


    /**
     * 특정 발언과 이슈 연결 (이벤트 발행 포함)
     * 이슈의 relatedStatementIds 목록에 발언 ID를 추가하고 연결 이벤트를 발행
     * @param issueId
     * @param statementId
     */
    @Transactional
    public void linkIssueToStatement(String issueId, String statementId) {
        log.info("이슈-발언 연결 시작: issueId={}, statementId={}", issueId, statementId);

        // 이슈에 발언 ID 추가
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedStatement(statementId);
        issueRepository.save(issue);

        // 이벤트 발행
        eventPublisher.publish(new IssueLinkedToStatementEvent(issueId, statementId));

        log.info("이슈-발언 연결 완료 및 이벤트 발행: issueId={}, statementId={}", issueId, statementId);
    }

    /**
     * 부모 카테고리에 속한 이슈 목록 조회
     * 대분류 카테고리에 속하는 모든 하위 카테고리의 이슈들을 조회
     * @param categoryId
     * @param page
     * @param size
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssueByParentCategory(Long categoryId, int page, int size) {
        log.debug("부모 카테고리별 이슈 조회: categoryId={}, page={}, size={}", categoryId, page, size);

        List<IssueCategory> issueCategories = categoryService.getIssueCategoriesByParentCategory(categoryId);
        List<String> categoryCodes = issueCategories.stream()
                .map(IssueCategory::getCode)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<IssueDocument> issuePage =
                issueRepository.findByCategoryCodeInAndIsActiveTrueOrderByCreatedAtDesc(categoryCodes, pageable);

        return issuePage.map(IssueResponse::from);
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
     * 카테고리 코드 유효성 검증 (내부 전용)
     * 입력받은 카테고리 코드가 유효한지 확인하고 IssueCategory 객체를 반환
     * @param code
     * @return
     */
    private IssueCategory validateCategory(String code) {
        if (code == null || code.isEmpty()) {
            throw new InvalidCategoryException("카테고리 코드가 필요합니다");
        }

        try {
            return IssueCategory.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidCategoryException("유효하지 않은 카테고리 코드: " + code);
        }
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


}
