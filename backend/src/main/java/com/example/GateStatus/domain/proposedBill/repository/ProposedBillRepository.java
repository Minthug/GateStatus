package com.example.GateStatus.domain.proposedBill.repository;

import com.example.GateStatus.domain.proposedBill.ProposedBill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProposedBillRepository extends JpaRepository<ProposedBill, Long> {

    Optional<ProposedBill> findByBillId(String billId);

}
