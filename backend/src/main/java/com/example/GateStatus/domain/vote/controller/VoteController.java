package com.example.GateStatus.domain.vote.controller;

import com.example.GateStatus.domain.vote.dto.BillDetailDTO;
import com.example.GateStatus.domain.vote.dto.BillVoteDTO;
import com.example.GateStatus.domain.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/figure/{figureId}")
    public ResponseEntity<List<BillVoteDTO>> getVotesFigureId(@PathVariable Long figureId) {
        return ResponseEntity.ok(voteService.getVotesByFigureId(figureId));
    }

    @GetMapping("/bill/{billNo}")
    public ResponseEntity<BillDetailDTO> getBillDetail(@PathVariable String billNo) {
        return ResponseEntity.ok(voteService.getBillDetail(billNo));
    }

    @GetMapping("/politician/votes")
    public ResponseEntity<Page<BillVoteDTO>> getVotesByPoliticianName(@RequestParam String name,
                                                                      @PageableDefault(size = 20, sort = "voteDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(voteService.getVotesByFigureName(name, pageable));
    }
}
