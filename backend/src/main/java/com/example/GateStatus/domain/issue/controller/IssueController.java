package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private static final String BASE_URL = "/v1/issues";
    private final IssueService issueService;
    private final IssueCacheService issueCacheService;
    private final IssueRepository issueRepository;

}
