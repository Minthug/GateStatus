package com.example.GateStatus.domain.vote.repository;

import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.VoteResultType;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByFigureId(Long figureId);

    List<Vote> findByBillId(Long billId);

    boolean existsByFigureIdAndBillBillNo(Long figureId, String billNo);

    List<Vote> findByFigureIdAndVoteResult(Long figureId, VoteResultType voteResult);

    List<Vote> findByVoteDateAfter(LocalDate date);

    List<Vote> findByMeetingName(String meetingName);

    List<Vote> findByFigureIdAndVoteDateBetween(Long figureId, LocalDate startDate, LocalDate endDate);

    List<Vote> findByFigureIdAndBillBillIdInAndVoteDateBetween(Long figureId, List<String> billIds, LocalDate startDate, LocalDate endDate);

    List<Object[]> countVotesByResult(Long figureId);

}
