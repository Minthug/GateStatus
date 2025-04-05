package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.exception.NotFoundIssueException;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.request.FindIssueDetailCommand;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueCommand;
import com.example.GateStatus.domain.issue.repository.request.UpdateIssueCommand;
import com.example.GateStatus.domain.issue.repository.response.FindIssueDetailResponse;
import com.example.GateStatus.domain.issue.repository.response.FindIssueDetailResponse.IssueDetailResponse;
import com.example.GateStatus.domain.issue.repository.response.FindIssuesResponse;
import com.example.GateStatus.domain.issue.repository.response.FindIssuesResponse.FindIssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCacheService issueCacheService;

    @Transactional
    public Long registerIssue(RegisterIssueCommand command) {
        Issue issue = new Issue(command.title(), command.content());
        Issue registered = issueRepository.save(issue);

        return registered.getId();
    }

    @Transactional(readOnly = true)
    public FindIssuesResponse findIssue() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<Issue> issuePage = issueRepository.findAllByOrderByCreatedDateDesc(pageable);

        List<Issue> issues = issuePage.getContent();

        return FindIssuesResponse.of(issues.stream()
                .map(issue -> new FindIssueResponse(
                        issue.getId(),
                        issue.getTitle(),
                        issue.getContent()
                )).collect(Collectors.toList()));
    }


    @Transactional(readOnly = true)
    public FindIssueDetailResponse findIssueDetail(FindIssueDetailCommand command) {

        Issue issue = issueCacheService.findIssueById(command.issueId());

        issueRepository.incrementViewCount(issue.getId());
        issueCacheService.incrementViewCount(issue.getId());

        IssueDetailResponse issueDetailResponse = new IssueDetailResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getContent()
        );

        return FindIssueDetailResponse.of(issueDetailResponse);
    }

    @Transactional
    public void updateIssue(Long issueId, UpdateIssueCommand command) {

        Issue issue  = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundIssueException("존재하지 않은 이슈 입니다"));

        issue.update(
                command.title(),
                command.content(),
                command.thumbnail(),
                command.tags(),
                command.viewCount(),
                command.isHot());

        issueCacheService.updateIssueCache(issue);
    }

    @Transactional
    public void deleteIssue(Long issueId) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new NotFoundIssueException("존재하지 않은 이슈 입니다"));

        issueRepository.delete(issue);

        issueCacheService.evictIssueCache(issue);
    }
}
