package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.issue.Issue;
import com.example.GateStatus.domain.issue.exception.NotFoundIssueException;
import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueCommand;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueRequest;
import com.example.GateStatus.domain.issue.repository.response.FindIssueDetailResponse;
import com.example.GateStatus.domain.issue.repository.response.FindIssueDetailResponse.IssueDetailResponse;
import com.example.GateStatus.domain.issue.repository.response.FindIssuesResponse;
import com.example.GateStatus.domain.issue.repository.response.IssueRedisDto;
import com.example.GateStatus.domain.issue.service.IssueCacheService;
import com.example.GateStatus.domain.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private static final String BASE_URL = "/v1/issues";
    private final IssueService issueService;
    private final IssueCacheService issueCacheService;
    private final IssueRepository issueRepository;

}
