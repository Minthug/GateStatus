package com.example.GateStatus.domain.issue.service;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;

    @Transactional
    public Long registerIssue(RegisterIssueCommand command) {
        Issue issue = new Issue(command.title(), command.content());
        Issue registered = issueRepository.save(issue);

        return registered.getId();
    }

    @Transactional
    public FindIssueResponse
}
