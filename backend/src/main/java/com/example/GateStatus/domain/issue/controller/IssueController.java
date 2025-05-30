package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.category.service.CategoryService;
import com.example.GateStatus.domain.issue.service.IssueService;
import com.example.GateStatus.domain.issue.service.request.AutoLinkRequest;
import com.example.GateStatus.domain.issue.service.request.LinkRequest;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import com.example.GateStatus.domain.issue.service.response.LinkResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import java.time.LocalDateTime;
import java.util.List;
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
    public ResponseEntity<Page<IssueResponse>> searchIssues(@RequestParam String q,
                                                     @RequestParam(defaultValue = "contains") String type,
                                                     @PageableDefault(size = 10) Pageable pageable) {

        log.info("이슈 검색: q={}, type={}", q, type);

        Page<IssueResponse> result = issueService.searchIssues(q, type, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * 카테고리별 이슈 목록 조회
     * @param categoryCode
     * @param pageable
     * @return
     */
    @GetMapping("/category/{categoryCode}")
    public ResponseEntity<Page<IssueResponse>> getIssuesByCategory(@PathVariable String categoryCode,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        log.info("카테고리별 이슈 조회: {}", categoryCode);
        Page<IssueResponse> issues = issueService.getIssuesByCategory(categoryCode, pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 인기 이슈 목록 조회
     * @param pageable
     * @return
     */
    @GetMapping("/hot")
    public ResponseEntity<Page<IssueResponse>> getHotIssues(@PageableDefault(size = 10) Pageable pageable) {
        log.info("인기 이슈 조회 ");
        Page<IssueResponse> issues = issueService.getHotIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<IssueResponse>> getRecentIssues(@PageableDefault(size = 10) Pageable pageable) {
        log.info("최근 이슈 조회");
        Page<IssueResponse> result = issueService.getRecentIssues(pageable);
        return ResponseEntity.ok(result);
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
                                                            @Valid @RequestBody LinkRequest request) {


        log.info("이슈 ID로 연결: issueId={}, type={}, resourceId={}",
                issueId, request.resourceType(), request.resourceId());

        issueService.linkIssueToResource(issueId, request.resourceType(), request.resourceId());

        String message = switch (request.resourceType().toUpperCase()) {
            case "BILL" -> "이슈와 법안이 연결되었습니다";
            case "STATEMENT" -> "이슈와 발언이 연결되었습니다";
            case "FIGURE" -> "이슈와 정치인이 연결되었습니다";
            case "NEWS" -> "이슈와 뉴스가 연결되었습니다";
            default -> "리소스가 연결되었습니다";
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
    // 🔗 관련 데이터 조회 API들
    // ============================================

    /**
     * ✅ 수정: Service의 통합 연결 메서드 사용
     */
    @PostMapping("/{issueId}/links")
    public ResponseEntity<LinkResponse> linkIssueToResource(
            @PathVariable String issueId,
            @Valid @RequestBody LinkRequest request) {  // ✅ @Valid 추가

        log.info("이슈 연결: issueId={}, type={}, resourceId={}",
                issueId, request.resourceType(), request.resourceId());

        // ✅ 수정: Service의 통합 메서드 사용
        issueService.linkIssueToResource(issueId, request.resourceType(), request.resourceId());

        String message = switch (request.resourceType().toUpperCase()) {
            case "BILL" -> "이슈와 법안이 연결되었습니다";
            case "STATEMENT" -> "이슈와 발언이 연결되었습니다";
            case "FIGURE" -> "이슈와 정치인이 연결되었습니다";
            case "NEWS" -> "이슈와 뉴스가 연결되었습니다";
            default -> "리소스가 연결되었습니다";
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

    /**
     * 추가: 리소스별 이슈 조회 (통합 API)
     */
    @GetMapping("/by-{resourceType}/{resourceId}")
    public ResponseEntity<List<IssueResponse>> getIssuesByResource(@PathVariable String resourceType,
                                                                   @PathVariable String resourceId) {

        log.info("리소스별 이슈 조회: type={}, id={}", resourceType, resourceId);

        List<IssueResponse> issues = issueService.getIssuesByResource(resourceType.toUpperCase(), resourceId);
        return ResponseEntity.ok(issues);
    }

        /**
         * 관련 이슈 조회
         */
    @GetMapping("/{issueId}/related")
    public ResponseEntity<List<IssueResponse>> getRelatedIssues(@PathVariable String issueId,
                                                                @RequestParam(defaultValue = "5") int limit) {
        log.info("관련 이슈 조회: issueId={}, limit={}", issueId, limit);

        List<IssueResponse> result = issueService.findRelatedIssues(issueId, limit);
        return ResponseEntity.ok(result);
    }

    // ============================================
    // 🤖 자동화 기능 (관리자용)
    // ============================================

    @PostMapping("/auto-link/news/{newsId}")
    public ResponseEntity<Map<String, Object>> autoLinkNewsToIssues(@PathVariable String newsId,
                                                                    @RequestBody AutoLinkRequest request) {

        log.info("뉴스 자동 연결 요청: newsId={}", newsId);

        if (request.title() == null || request.content() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "title과 content가 필요합니다"));
        }

        issueService.autoLinkNewsToIssues(newsId, request.title(), request.content());

        Map<String, Object> response = Map.of(
                "message", "뉴스 자동 연결이 시작되었습니다",
                "newsId", newsId,
                "status", "PROCESSING",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.accepted().body(response);
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