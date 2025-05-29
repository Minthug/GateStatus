package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.category.service.CategoryService;
import com.example.GateStatus.domain.issue.service.IssueService;
import com.example.GateStatus.domain.issue.service.request.LinkRequest;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import com.example.GateStatus.domain.issue.service.response.LinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.directory.SearchResult;
import java.time.LocalDateTime;
import java.util.Map;

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

    @GetMapping("/category/{categoryCode}")
    public ResponseEntity<Page<IssueResponse>> getIssuesByCategory(@PathVariable String categoryCode,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        log.info("카테고리별 이슈 조회: {}", categoryCode);
        Page<IssueResponse> issues = issueService.getIssuesByCategory(categoryCode, pageable);
        return ResponseEntity.ok(issues);
    }


    // ============================================
    // 🛠️ 개발자용 API (ID 기반) - 유지
    // ============================================

    /**
     * 개발자용 - ID로 직접 조회 (빠름)
     */
    @GetMapping("/{issueId}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable String issueId) {
        log.info("이슈 ID로 조회: {}", issueId);

        if (!isValidObjectId(issueId)) {
            throw new IllegalArgumentException("잘못된 이슈 ID 형식입니다: " + issueId);
        }

        IssueResponse issue = issueService.getIssue(issueId);
        return ResponseEntity.ok(issue);
    }

    /**
     * 이슈와 리소스 연결
     * POST /v1/issues/{issueId}/links
     */
    @PostMapping("/{issueId}/links")
    public ResponseEntity<LinkResponse> linkIssueToResponse(@PathVariable String issueId,
                                                                   @RequestBody LinkRequest request) {


        log.info("이슈 ID로 연결: issueId={}, type={}, resourceId={}",
                issueId, request.resourceType(), request.resourceId());

        if (!request.isValid()) {
            throw new IllegalArgumentException("잘못된 요청 데이터입니다");
        }

        String message = switch (request.resourceType()) {
            case "BILL" -> {
                issueService.linkIssuesToBill(issueId, request.resourceId());
                yield "이슈와 법안이 연결되었습니다";
            }

            case "STATEMENT" -> {
                issueService.linkIssueToStatement(issueId, request.resourceId());
                yield "이슈와 발언이 연결되었습니다";
            }

            case "FIGURE" -> {
                Long figureId = request.getFigureId();
                issueService.linkIssuesToFigure(issueId, figureId);
                yield "이슈와 정치인이 연결되었습니다";
            }

            case "NEWS" -> {
                issueService.linkNewsToIssue(issueId, request.resourceId());
                yield "이슈와 뉴스가 연결되었습니다";
            }
            default -> throw new IllegalArgumentException("지원하지 않는 리소스 타입: " + request.resourceType());
        };
        LinkResponse response = new LinkResponse(
                message,
                issueId,
                request.resourceType(),
                request.resourceId(),
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }


    // ============================================
    // 🔧 유틸리티 메서드들
    // ============================================

    /**
     * MongoDB ObjectID 형식 검증
     */
    private boolean isValidObjectId(String id) {
        return id != null && id.matches("^[0-9a-fA-F]{24}$");
    }
}