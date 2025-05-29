package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.category.service.CategoryService;
import com.example.GateStatus.domain.issue.IssueCategory;
import com.example.GateStatus.domain.issue.service.IssueService;
import com.example.GateStatus.domain.issue.service.request.IssueRequest;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import javax.naming.directory.SearchResult;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/issues")
@RequiredArgsConstructor
@Slf4j
public class IssueController {

    private final IssueService issueService;
    private final CategoryService categoryService;


    // ============================================
    // 🎯 사용자 친화적 API (이슈 이름으로 검색)
    // ============================================

    /**
     * 이슈 이름으로 검색
     * GET /v1/issues/search-by-name?name=부동산%20정책
     *
     * %20이란? 공백때문에 URL이 깨지기 때문에 공백을 %20으로 인코딩해야 전체 문자열 인식
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<IssueResponse> getIssueName(@RequestParam String name) {
        log.info("이슈 이름으로 검색: {}", name);
        IssueResponse issue = issueService.getIssueByName(name);
        return ResponseEntity.ok(issue);
    }


    @GetMapping("/search")
    public ResponseEntity<SearchResult> searchIssues(@RequestParam String q,
                                                     @RequestParam(defaultValue = "contains") String type,
                                                     @PageableDefault(size = 10) Pageable pageable) {

        log.info("이슈 검색: q={}, type={}", q, type);

        SearchResult result = switch (type) {
            case "exact" -> issueService.findByExactName(q, pageable);
            case "contains" -> issueService.searchByKeyword(q, pageable);
            case "fuzzy" -> issueService.fuzzySearch(q, pageable);
            default -> issueService.searchByKeyword(q, pageable);
        };
        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<IssueResponse> getIssueBySlug(@PathVariable String slug) {
        // 슬러그는 영문/숫자/하이픈만 사용하므로 인코딩 문제 없음
        log.info("슬러그로 검색: {}", slug);

        IssueResponse response = issueService.getIssueBySlug(slug);
        return ResponseEntity.ok(response);
    }
}