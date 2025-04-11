package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ProposedBill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "figure_id")
    private Figure proposer;

    private String billId;
    private String billNo;
    private String billName;
    private LocalDate proposeDate;
    private String summary;
    private String content;
    private String billUrl;

    @Enumerated(EnumType.STRING)
    private BillStatus billStatus;

    private LocalDate processDate;
    private String processResult;

    @ElementCollection
    @CollectionTable(name = "bill_coproposers", joinColumns = @JoinColumn(name = "bill_no"))
    private List<String> coProposers;
    private String committee;

    private Integer viewCount = 0;

    public void update(BillStatus billStatus, String processResult, LocalDate processDate) {
        this.billStatus = billStatus;
        this.processResult = processResult;
        this.processDate = processDate;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
