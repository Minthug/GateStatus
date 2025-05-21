package com.example.GateStatus.domain.vote.repository;

import com.example.GateStatus.domain.figure.FigureParty;
import com.example.GateStatus.domain.vote.Vote;
import com.example.GateStatus.domain.vote.VoteResultType;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Page<Vote> findByFigureId(Long figureId, Pageable pageable);

    List<Vote> findByBillNo(String billNo);

    List<Vote> findByBillId(Long billId);

    boolean existsByFigureIdAndBillBillNo(Long figureId, String billNo);

    List<Vote> findByFigureIdAndVoteResult(Long figureId, VoteResultType voteResult);

    List<Vote> findByVoteDateAfter(LocalDate date);

    List<Vote> findByMeetingName(String meetingName);

    List<Vote> findByFigureIdAndVoteDateBetween(Long figureId, LocalDate startDate, LocalDate endDate);

    List<Vote> findByFigurePartyAndVoteDateBetween(FigureParty party, LocalDate start, LocalDate end);

    List<Vote> findByFigureIdAndBillBillIdInAndVoteDateBetween(Long figureId, List<String> billIds, LocalDate startDate, LocalDate endDate);

    // 유사 법안 검색 (키워드 기반)
    @Query("SELECT v FROM Vote v JOIN v.bill b WHERE LOWER(b.billName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vote> findByBillNameContaining(@Param("keyword") String keyword);

//    // 이슈 카테고리별 투표 검색
//    @Query("SELECT v FROM Vote v JOIN v.bill b JOIN b.issues i WHERE i.category = :category")
//    List<Vote> findByIssueCategory(@Param("category") String category);

    @Query("SELECT v.voteResult as result, COUNT(v) as count " +
            "FROM Vote v " +
            "WHERE v.figure.id = :figureId " +
            "GROUP BY v.voteResult")
    List<Object[]> countVotesByResult(Long figureId);

}
