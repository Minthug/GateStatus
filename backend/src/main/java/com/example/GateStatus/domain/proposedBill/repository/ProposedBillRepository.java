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

    List<ProposedBill> findByProposerIdAndProposeDateBetween(Long proposerId, LocalDate startDate, LocalDate endDate);

    List<ProposedBill> findByProposerIdAndIdInAndProposeDateBetween(Long proposerId, List<String> billIds, LocalDate startDate, LocalDate endDae);


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
}
