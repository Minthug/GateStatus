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

    @PostMapping
    public ResponseEntity<Void> registerIssue(@RequestBody RegisterIssueRequest request) {
        RegisterIssueCommand command = RegisterIssueCommand.of(request.title(), request.content());
        Long issueId = issueService.registerIssue(command);
        URI location = URI.create(BASE_URL + issueId);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<FindIssuesResponse> findIssues() {
        List<IssueRedisDto> recentIssues = issueCacheService.getRecentIssues(100);

        if (!recentIssues.isEmpty()) {
            return ResponseEntity.ok(FindIssuesResponse.fromRedis(recentIssues));
        }

        FindIssuesResponse response = issueService.findIssue();
        response.issues().forEach(issue -> issueCacheService.cacheIssueInfo(IssueRedisDto.from(issue, LocalDateTime.now())));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{issueId}")
    public ResponseEntity<FindIssueDetailResponse> findIssue(@PathVariable(value = "issueId") final Long issueId) {
        return issueCacheService.getIssueInfo(issueId)
                .map(issueRedisDto -> {
                    IssueDetailResponse issueDetail = new IssueDetailResponse(
                            issueRedisDto.issueId(),
                            issueRedisDto.title(),
                            issueRedisDto.content());
                    return ResponseEntity.ok(FindIssueDetailResponse.of(issueDetail));
                })
                .orElseGet(() -> {
                    Issue issue = issueRepository.findById(issueId)
                            .orElseThrow(() -> new NotFoundIssueException("Issue not found"));

                    IssueDetailResponse issueDetailResponse = new IssueDetailResponse(
                            issue.getId(),
                            issue.getTitle(),
                            issue.getContent());

                    issueCacheService.cacheIssueInfo(IssueRedisDto.from(issue));
                    return ResponseEntity.ok(FindIssueDetailResponse.of(issueDetailResponse));
                });
    }
}
