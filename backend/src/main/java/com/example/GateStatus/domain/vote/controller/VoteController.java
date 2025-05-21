package com.example.GateStatus.domain.vote.controller;

import com.example.GateStatus.domain.vote.dto.BillDetailDTO;
import com.example.GateStatus.domain.vote.dto.BillVoteDTO;
import com.example.GateStatus.domain.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
