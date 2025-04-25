package com.example.GateStatus.domain.issue.service;

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
    private final EventPublisher eventPublisher;

    /**
     * 이슈 상세 조회
     * @param id
     * @return
     */
    @Transactional
    public IssueResponse getIssue(String id) {
        IssueDocument issue = findByIssueById(id);
        issue.incrementViewCount();
        issueRepository.save(issue);
        return IssueResponse.from(issue);
    }

    /**
     * 활성화된 이슈만 찾기
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
     * @param categoryCode
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByCategory(String categoryCode, Pageable pageable) {
        return issueRepository.findByCategoryCodeAndIsActiveTrue(categoryCode, pageable)
                .map(IssueResponse::from);
    }

    /**
     * 인기(핫) 이슈 목록 조회
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
        return issueRepository.findIssuesByStatementId(statementId)
                .stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 키워드 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> searchIssues(String keyword, Pageable pageable) {
        return issueRepository.searchByKeyword(keyword, pageable)
                .map(IssueResponse::from);
    }

    /**
     * 태그로 이슈 검색
     * @param tag
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getIssuesByTag(String tag, Pageable pageable) {
        return issueRepository.findByTagsContainingAndIsActiveTrue(tag, pageable)
                .map(IssueResponse::from);
    }

    /**
     * 최근 이슈 목록 조회
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Page<IssueResponse> getRecentIssues(Pageable pageable) {
        return issueRepository.findByIsActiveTrueAndOrderByCreatedAtDesc(pageable)
                .map(IssueResponse::from);
    }

    /**
     * 새 이슈 생성
     * @param request
     * @return
     */
    @Transactional
    public IssueResponse createIssue(IssueRequest request) {

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
                .relatedStatementIds(request.relatedStatementIds())
                .relatedBillIds(request.relatedBillIds())
                .relatedFigureIds(request.relatedFigureIds())
                .tags(request.tags())
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        IssueDocument savedIssue = issueRepository.save(document);
        return IssueResponse.from(savedIssue);
    }

    /**
     * 이슈 정보 업데이트
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public IssueResponse updateIssue(String id, IssueRequest request) {
        IssueDocument issue = findByIssueById(id);

        issue.update(
                request.name(),
                request.description(),
                request.categoryCode(),
                request.categoryName(),
                request.keywords(),
                request.thumbnailUrl(),
                request.tags(),
                request.isActive(),
                request.isHot()
        );

        if (request.relatedStatementIds() != null) {
            issue.setRelatedStatementIds(request.relatedStatementIds());
        }

        if (request.relatedBillIds() != null) {
            issue.setRelatedBillIds(request.relatedBillIds());
        }

        if (request.relatedFigureIds() != null) {
            issue.setRelatedFigureIds(request.relatedFigureIds());
        }

        if (request.priority() != null) {
            issue.setPriority(request.priority());
        }

        if (request.parentIssueId() != null) {
            issue.setParentIssueId(request.parentIssueId());
        }

        issue.setUpdatedAt(LocalDateTime.now());
        IssueDocument updatedIssue = issueRepository.save(issue);
        return IssueResponse.from(updatedIssue);
    }

    /**
     * 이슈 삭제 (논리적 삭제)
     * @param id
     */
    @Transactional
    public void deleteIssue(String id) {
        IssueDocument issue = findByIssueById(id);
        issue.setIsActive(false);
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);
        log.info("이슈가 비활성화 되었습니다");
    }

    /**
     * 물리적 이슈 삭제 (관리자 전용)
     * @param id
     */
    @Transactional
    public void hardDeleteIssue(String id) {
        issueRepository.deleteById(id);
        log.info("이슈가 완전히 삭제 되었습니다: {}", id);
    }


    /**
     * 관련 이슈 찾기
     * @param issueId
     * @param limit
     * @return
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> findRelatedIssue(String issueId, int limit) {
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

                // 이미 포함된 이슈 제외하고 추가
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

        return relatedByCategory.stream()
                .map(IssueResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 발언에 관련된 이슈 연결
     * @param issueId
     * @param statementId
     */
    @Transactional
    public void linkIssuesToStatement(String issueId, String statementId) {
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedStatement(statementId);
        issueRepository.save(issue);
        log.info("이슈 {} 에 발언 {} 연결됨", issueId, statementId);
    }

    /**
     * 특정 법안에 관련된 이슈 연결 **
     * @param issueId
     * @param billId
     */
    @Transactional
    public void linkIssuesToBill(String issueId, String billId) {
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedBill(billId);
        issueRepository.save(issue);
        log.info("이슈 {} 에 법안 {} 연결됨", issueId ,billId);
    }

    /**
     * 특정 정치인에 관련된 이슈 연결
     * @param issueId
     * @param figureId
     */
    @Transactional
    public void linkIssuesToFigure(String issueId, Long figureId) {
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedFigure(figureId);
        issueRepository.save(issue);
        log.info("이슈 {} 에 정치인 {} 연결됨", issueId, figureId);
    }


    @Transactional
    public void linkIssueToStatement(String issueId, String statementId) {
        // 이슈에 발언 ID 추가
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedStatement(statementId);
        issueRepository.save(issue);

        // 이벤트 발행
        eventPublisher.publish(new IssueLinkedToStatementEvent(issueId, statementId));
    }

    /**
     * 내부용 ID 찾기
     * @param id
     * @return
     */
    private IssueDocument findByIssueById(String id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundIssueException("해당 이슈가 존재하지 않습니다" + id));
    }

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
}
