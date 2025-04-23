package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.IssueDocument;
import com.example.GateStatus.domain.issue.exception.NotFoundIssueException;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import com.example.GateStatus.global.config.EventListner.EventPublisher;
import com.example.GateStatus.global.config.EventListner.IssueLinkedToStatementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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



    @Transactional
    public void linkIssueToStatement(String issueId, String statementId) {
        IssueDocument issue = findByIssueById(issueId);
        issue.addRelatedStatement(statementId);
        issueRepository.save(issue);

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
}
