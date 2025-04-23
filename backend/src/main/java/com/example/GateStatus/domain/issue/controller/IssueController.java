package com.example.GateStatus.domain.issue.controller;

import com.example.GateStatus.domain.issue.service.IssueService;
import com.example.GateStatus.domain.issue.service.request.IssueRequest;
import com.example.GateStatus.domain.issue.service.response.IssueResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import java.util.List;

@RestController
@RequestMapping("/v1/issues")
@RequiredArgsConstructor
@Slf4j
public class IssueController {

    private final IssueService issueService;

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


}
