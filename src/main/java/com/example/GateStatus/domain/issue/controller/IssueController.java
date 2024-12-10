package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.issue.repository.IssueRepository;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueCommand;
import com.example.GateStatus.domain.issue.repository.request.RegisterIssueRequest;
import com.example.GateStatus.domain.issue.repository.response.FindIssuesResponse;
import com.example.GateStatus.domain.issue.repository.response.IssueRedisDto;
import com.example.GateStatus.domain.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private static final String BASE_URL = "/v1/issues";
    private final IssueService issueService;
    private final IssueRepository issueRepository;

    @PostMapping
    public ResponseEntity<Void> registerIssue(@RequestBody RegisterIssueRequest request) {
        RegisterIssueCommand command = RegisterIssueCommand.of(request.title(), request.content());
        Long issueId = issueService.registerIssue(command);
        URI location = URI.create(BASE_URL + issueId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<FindIssuesResponse> findIssues() {
        List<IssueRedisDto> recentIssues = issueService.
    }
}
