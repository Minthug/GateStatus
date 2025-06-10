package com.example.GateStatus.domain.proposedBill.repository;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.proposedBill.BillStatus;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProposedBillRepository extends JpaRepository<ProposedBill, Long> {

    Optional<ProposedBill> findByBillId(String billId);

    Optional<ProposedBill> findByBillNo(String BillNo);

    List<ProposedBill> findByProposer(Figure proposer);

    Page<ProposedBill> findByProposer(Figure proposer, Pageable pageable);

    Page<ProposedBill> findByBillNameContaining(String keyword, Pageable pageable);

    List<ProposedBill> findByBillStatus(BillStatus status);

    @Query("SELECT p FROM ProposedBill p WHERE p.proposeDate BETWEEN :startDate AND :endDate")
    List<ProposedBill> findByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM ProposedBill p ORDER BY p.viewCount DESC")
    List<ProposedBill> findTopByOrderByViewCountDesc(Pageable pageable);

    /**
     * 여러 정치인이 발의한 법안을 한번에 조회
     */
    @Query("SELECT b FROM ProposedBill b WHERE b.proposer.id IN :proposerIds AND b.proposeDate BETWEEN :startDate AND :endDate")
    List<ProposedBill> findByProposerIdInAndProposeDateBetween(
            @Param("proposerIds") List<Long> proposerIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 법안 ID들에 해당하는 여러 정치인의 법안 조회
     */
    @Query("SELECT b FROM ProposedBill b WHERE b.proposer.id IN :proposerIds AND b.id IN :billIds AND b.proposeDate BETWEEN :startDate AND :endDate")
    List<ProposedBill> findByProposerIdInAndIdInAndProposeDateBetween(
            @Param("proposerIds") List<Long> proposerIds,
            @Param("billIds") List<Long> billIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query("SELECT p.billStatus as status, COUNT (p) as count " +
            "FROM ProposedBill p " +
            "WHERE p.proposer.id = :proposerId " +
            "GROUP BY p.billStatus" )
    List<Object[]> countBillsByStatus(@Param("proposerId") Long proposerId);


    @Query("SELECT TO_CHAR(p.proposeDate, 'YYYY-MM') as month, COUNT(p) as count " +
            "FROM ProposedBill p " +
            "WHERE p.proposer.id = :proposerId AND p.proposeDate BETWEEN :startDate AND :endDate " +
            "GROUP BY TO_CHAR(p.proposeDate, 'YYYY-MM') " +
            "ORDER BY TO_CHAR(p.proposeDate, 'YYYY-MM')") // 수정: 명시적 정렬 기준 사용
    List<Object[]> countBillsByMonth(@Param("proposerId") Long proposerId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);


//    List<KeywordCount> findTopKeywordsByDateRange(Long figureId, List<String> stopwords, LocalDate startDate, LocalDate endDate);


//    @Query("SELECT p FROM ProposedBill p WHERE p.categoryCode = :categoryCode AND p.proposeDate BETWEEN :startDate AND :endDate")
//    List countByCategoryAndDateRange(@Param("categoryId") String categoryId,
//                                     @Param("startDate") LocalDate startDate,
//                                     @Param("endDate") LocalDate endDate);


    @Query("SELECT p.billStatus, COUNT(p) FROM ProposedBill p " +
            "WHERE p.proposer.id = :figureId AND p.proposeDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.billStatus")
    List<Object[]> countBillsByStatusAndDateRange(Long figureId, LocalDate startDate, LocalDate endDate);


    @Query("SELECT COUNT(p) FROM ProposedBill p " +
            "WHERE p.proposer.id = :figureId AND p.proposeDate BETWEEN :startDate AND :endDate")
    long countByProposerIdAndDateRange(@Param("figureId") Long figureId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);



//    @Query("SELECT " +
//            "FUNCTION('DATE_FORMAT', p.proposeDate, '%Y-%m') as month, " +
//            "COUNT(p) as total, " +
//            "SUM(CASE WHEN p.billStatus = com.example.GateStatus.domain.proposedBill.BillStatus.PASSED THEN 1 ELSE 0 END) as passed " +
//            "FROM ProposedBill p " +
//            "WHERE p.proposer.id = :figureId AND p.proposeDate BETWEEN :startDate AND :endDate " +
//            "GROUP BY FUNCTION('DATE_FORMAT', p.proposeDate, '%Y-%m') " +
//            "ORDER BY FUNCTION('DATE_FORMAT', p.proposeDate, '%Y-%m')")
//    List<Object[]> countBillsByMonthDetailed(@Param("figureId") Long figureId,
//                                             @Param("startDate") LocalDate startDate,
//                                             @Param("endDate") LocalDate endDate);


//    @Query("SELECT p FROM ProposedBill p " +
//            "WHERE p.proposer.id = :figureId AND p.proposeDate BETWEEN :startDate AND :endDate " +
//            "AND (p.billName LIKE %:keyword1% OR p.billName LIKE %:keyword2% OR p.summary LIKE %:keyword1%)")
//    List<ProposedBill> findByCategoryKeywords(@Param("figureId") Long figureId,
//                                              @Param("keyword1") String keyword1,
//                                              @Param("keyword2") String keyword2,
//                                              @Param("startDate") LocalDate startDate,
//                                              @Param("endDate") LocalDate endDate);

}
