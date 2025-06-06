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
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * 특정 정치인의 모든 투표 내역 조회
     */
    List<Vote> findByFigureId(Long figureId);

    /**
     * 특정 정치인의 투표 내역 페이징 조회
     */
    Page<Vote> findByFigureId(Long figureId, Pageable pageable);

    /**
     * 특정 법안번호로 투표 내역 조회
     */
    List<Vote> findByBillBillNo(String billNo);  // 수정: findByBillNo → findByBillBillNo

    /**
     * 특정 법안 ID로 투표 내역 조회
     */
    List<Vote> findByBillId(Long billId);

    /**
     * 특정 정치인이 특정 법안에 투표했는지 확인
     */
    boolean existsByFigureIdAndBillBillNo(Long figureId, String billNo);

    /**
     * 특정 정치인의 특정 투표 결과만 조회
     */
    List<Vote> findByFigureIdAndVoteResult(Long figureId, VoteResultType voteResult);

    /**
     * 특정 날짜 이후의 투표 내역 조회
     */
    List<Vote> findByVoteDateAfter(LocalDate date);

    /**
     * 특정 회의명으로 투표 내역 조회
     */
    List<Vote> findByMeetingName(String meetingName);

    /**
     * 특정 정치인의 기간별 투표 내역 조회
     */
    List<Vote> findByFigureIdAndVoteDateBetween(Long figureId, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 정당의 기간별 투표 내역 조회
     */
    List<Vote> findByFigureFigurePartyAndVoteDateBetween(FigureParty party, LocalDate start, LocalDate end);

   /*
    * 여러 정치인의 투표를 한번에 조회
    */
    @Query("SELECT v FROM Vote v WHERE v.figure.id IN :figureIds AND v.voteDate BETWEEN :startDate AND :endDate")
    List<Vote> findByFigureIdInAndVoteDateBetween(
            @Param("figureIds") List<Long> figureIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 법안들에 대한 여러 정치인의 투표 조회
     */
    @Query("SELECT v FROM Vote v WHERE v.figure.id IN :figureIds AND v.bill.billNo IN :billIds AND v.voteDate BETWEEN :startDate AND :endDate")
    List<Vote> findByFigureIdInAndBillBillNoInAndVoteDateBetween(
            @Param("figureIds") List<Long> figureIds,
            @Param("billIds") List<String> billIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 법안명 키워드로 투표 내역 검색
     */
    @Query("SELECT v FROM Vote v JOIN v.bill b WHERE LOWER(b.billName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Vote> findByBillNameContaining(@Param("keyword") String keyword);

    /**
     * 법안명 키워드로 투표 내역 검색 (페이징)
     */
    @Query("SELECT v FROM Vote v JOIN v.bill b WHERE LOWER(b.billName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Vote> findByBillNameContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 정당별 기간별 투표 내역 조회 (페이징)
     */
    @Query("SELECT v FROM Vote v WHERE v.figure.figureParty = :party AND v.voteDate BETWEEN :startDate AND :endDate")
    Page<Vote> findByPartyAndDateRange(@Param("party") FigureParty party,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       Pageable pageable);

    /**
     * 특정 정치인의 투표 결과별 통계
     */
    @Query("SELECT v.voteResult as result, COUNT(v) as count " +
            "FROM Vote v " +
            "WHERE v.figure.id = :figureId " +
            "GROUP BY v.voteResult")
    List<Object[]> countVotesByResult(@Param("figureId") Long figureId);

    /**
     * 특정 법안에 대한 모든 정치인의 투표 내역 조회 (페이징)
     */
    Page<Vote> findByBillBillNo(String billNo, Pageable pageable);

    /**
     * 특정 정치인의 특정 투표 결과만 페이징 조회
     */
    Page<Vote> findByFigureIdAndVoteResult(Long figureId, VoteResultType voteResult, Pageable pageable);


    @Query("SELECT v.voteResult, COUNT(v) FROM Vote v " +
            "WHERE v.figure.id = :figureId AND v.voteDate BETWEEN :startDate AND :endDate " +
            "GROUP BY v.voteResult")
    List<Object[]> countVotesByResultAndDateRange(@Param("figureId") Long figureId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(v) FROM Vote v " +
            "WHERE v.figure.id = :figureId AND v.voteDate BETWEEN :startDate AND :endDate")
    long countByFigureIdAndDateRange(@Param("figureId") Long figureId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);


    @Query("SELECT v FROM Vote v " +
            "WHERE v.figure.id = :figureId AND v.bill.id IN :billIds " +
            "AND v.voteDate BETWEEN :startDate AND :endDate")
    List<Vote> findByFigureIdAndBillIdsAndDateRange(@Param("figureId") Long figureId,
                                                    @Param("billIds") List<String> billIds,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);



}