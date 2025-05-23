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

    /**
     * 이슈 상세 조회
     * @param issueId
     * @return
     */
    @GetMapping("/{issueId}")
    public ResponseEntity<IssueResponse> getIssue(@PathVariable String issueId) {
        log.info("이슈 상세 조회 요청: {}", issueId);
        IssueResponse issue = issueService.getIssue(issueId);
        return ResponseEntity.ok(issue);
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
        log.info("카테고리별 이슈 목록 조회: {}", categoryCode);
        Page<IssueResponse> issues = issueService.getIssuesByCategory(categoryCode, pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 인기(핫) 이슈 목록 조회
     * @param pageable
     * @return
     */
    @GetMapping("/hot")
    public ResponseEntity<Page<IssueResponse>> getHotIssues(@PageableDefault(size = 10) Pageable pageable) {
        log.info("인기 이슈 목록 조회");
        Page<IssueResponse> issues = issueService.getHotIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 특정 정치인 관련 이슈 목록 조회
     * @param figureId
     * @param pageable
     * @return
     */
    @GetMapping("/figure/{figureId}")
    public ResponseEntity<Page<IssueResponse>> getIssuesByFigure(@PathVariable Long figureId,
                                                                 @PageableDefault(size = 10) Pageable pageable) {
        log.info("정치인 관련 이슈 목록 조회: {}", figureId);
        Page<IssueResponse> issues = issueService.getIssuesByFigure(figureId, pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 특정 법안 이슈 목록 조회
     * @return
     */
    @GetMapping("/bill/{billId}")
    public ResponseEntity<List<IssueResponse>> getIssuesByBill(@PathVariable String billId) {
        log.info("법안 관련 이슈 목록 조회: {}", billId);
        List<IssueResponse> issues = issueService.getIssuesByBill(billId);
        return ResponseEntity.ok(issues);
    }

    /**
     * 특정 발언 이슈 목록 조회
     * @param statementId
     * @return
     */
    @GetMapping("/statement{statementId}")
    public ResponseEntity<List<IssueResponse>> getIssuesByStatement(@PathVariable String statementId) {
        log.info("발언 관련 이슈 목록 조회: {}", statementId);
        List<IssueResponse> issues = issueService.getIssuesByStatement(statementId);
        return ResponseEntity.ok(issues);
    }

    /**
     * 키워드 검색
     * @param keyword
     * @param pageable
     * @return
     */
    @GetMapping("/search")
    public ResponseEntity<Page<IssueResponse>> searchIssues(@RequestParam String keyword,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        log.info("이슈 검색: {}", keyword);
        Page<IssueResponse> issues = issueService.searchIssues(keyword, pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 태그로 이슈 검색
     * @param tag
     * @param pageable
     * @return
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<Page<IssueResponse>> getIssuesByTag(@PathVariable String tag,
                                                              @PageableDefault(size = 10) Pageable pageable) {
        log.info("태그별 이슈 조회: {}", tag);
        Page<IssueResponse> issues = issueService.getIssuesByTag(tag, pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     * 최근 이슈 목록 조회
     * @param pageable
     * @return
     */
    @GetMapping("/recent")
    public ResponseEntity<Page<IssueResponse>> getRecentIssues(@PageableDefault(size = 10) Pageable pageable) {
        log.info("최근 이슈 목록 조회");
        Page<IssueResponse> issues = issueService.getRecentIssues(pageable);
        return ResponseEntity.ok(issues);
    }

    /**
     *  새 이슈 생성
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(@RequestBody IssueRequest request) {
        log.info("이슈 생성 요청: {}", request.name());
        IssueResponse issues = issueService.createIssue(request);
        return ResponseEntity.ok(issues);
    }

    /**
     * 이슈 정보 업데이트
     * @param issueId
     * @param request
     * @return
     */
    @PatchMapping("/{issueId}")
    public ResponseEntity<IssueResponse> updateIssue(@PathVariable String issueId,
                                                     @RequestBody IssueRequest request) {
        log.info("이슈 업데이트 요청: {}", issueId);
        IssueResponse issue = issueService.updateIssue(issueId, request);
        return ResponseEntity.ok(issue);
    }

    /**
     * 이슈 삭제 (논리적 삭제)
     * @param issueId
     * @return
     */
    @DeleteMapping("/{issueId}")
    public ResponseEntity<Void> deleteIssue(@PathVariable String issueId) {
        log.info("이슈 삭제 요청: {}", issueId);
        issueService.deleteIssue(issueId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 물리적 이슈 삭제 (관리자 전용)
     * @param issueId
     * @return
     */
    @DeleteMapping("/{issueId}/hard")
    public ResponseEntity<Void> hardDeleteIssue(@PathVariable String issueId) {
        log.info("이슈 영구 삭제 요청: {}", issueId);
        issueService.hardDeleteIssue(issueId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관련 이슈 찾기
     * @param issueId
     * @param limit
     * @return
     */
    @GetMapping("/{issueId}/related")
    public ResponseEntity<List<IssueResponse>> getRelatedIssue(@PathVariable String issueId,
                                                               @RequestParam(defaultValue = "5") int limit) {
        log.info("관련 이슈 조회: {}, 제한: {}", issueId, limit);
        List<IssueResponse> issues = issueService.findRelatedIssue(issueId, limit);
        return ResponseEntity.ok(issues);
    }

    /**
     * 특정 법안에 관련된 이슈 연결
     * @param issueId
     * @param billId
     * @return
     */
    @GetMapping("/{issueId}/link/{billId}")
    public ResponseEntity<Void> linkIssueToBill(@PathVariable String issueId,
                                                @PathVariable String billId) {
        log.info("이슈-법안 연결: {} - {}", issueId, billId);
        issueService.linkIssuesToBill(issueId, billId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 발언에 관련된 이슈 연결
     * @param issueId
     * @param statementId
     * @return
     */
    @GetMapping("/{issueId}/link/{statementId}")
    public ResponseEntity<Void> linkIssueToStatement(@PathVariable String issueId,
                                                     @PathVariable String statementId) {
        log.info("이슈-발언 연결: {} - {}", issueId, statementId);
        issueService.linkIssueToStatement(issueId, statementId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 정치인에 관련된 이슈 연결
     * @param issueId
     * @param figureId
     * @return
     */
    @GetMapping("/{issueId}/link/{figureId}")
    public ResponseEntity<Void> linkIssueToFigure(@PathVariable String issueId,
                                                  @PathVariable Long figureId) {
        log.info("이슈-정치인 연결: {} - {}", issueId, figureId);
        issueService.linkIssuesToFigure(issueId, figureId);
        return ResponseEntity.ok().build();
    }

    /**
     * 대분류 카테고리에 속한 이슈 카테고리 조회
     * @param categoryId
     * @return
     */
    @GetMapping("/categories/parent/{categoryId}")
    public ResponseEntity<Map<String, Object>> getIssueCategoriesByParent(@PathVariable Long categoryId) {
        List<IssueCategory> categories = categoryService.getIssueCategoriesByParentCategory(categoryId);

        List<Map<String, String>> categoryList = categories.stream()
                .map(cat -> Map.of(
                        "code", cat.getCode(),
                        "name", cat.getDisplayName()
                ))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("categoryId", categoryId);
        response.put("categories", categories);

        return ResponseEntity.ok(response);
    }

    /**
     * 대분류 카테고리에 속한 이슈 목록 조회
     * @param categoryId
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/by-parent-category/{categoryId}")
    public ResponseEntity<Page<IssueResponse>> getIssuesByParentCategory(@PathVariable Long categoryId,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        Page<IssueResponse> issues = issueService.getIssueByParentCategory(categoryId, page, size);

        return ResponseEntity.ok(issues);
    }
}

/**
 * Link 관련 3개의 메서드를 한개로 합치는 방법 구상하기
 */