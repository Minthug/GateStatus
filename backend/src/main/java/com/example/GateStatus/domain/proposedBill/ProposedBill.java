package com.example.GateStatus.domain.proposedBill;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
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

    @Column(unique = true)
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
    private String processResultCode;

    @ElementCollection
    @CollectionTable(name = "bill_coproposers", joinColumns = @JoinColumn(name = "bill_id"))
    @Column(name = "coproposer")
    private List<String> coProposers = new ArrayList<>();
    private String committee;

    private Integer viewCount = 0;

    public void incrementViewCount() {
        this.viewCount++;
    }

    // 아주 기본적인 상태 변경 메서드만 유지
    public void setBillStatus(BillStatus status) {
        this.billStatus = status;
    }

    public void setProposer(Figure proposer) {
        this.proposer = proposer;
    }

    public void setCoProposers(List<String> coProposers) {
        this.coProposers.clear();
        if (coProposers != null) {
            this.coProposers.addAll(coProposers);
        }
    }

    // 세터 메서드 추가
    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public void setBillName(String billName) {
        this.billName = billName;
    }

    public void setProposeDate(LocalDate proposeDate) {
        this.proposeDate = proposeDate;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setBillUrl(String billUrl) {
        this.billUrl = billUrl;
    }

    public void setProcessDate(LocalDate processDate) {
        this.processDate = processDate;
    }

    public void setProcessResult(String processResult) {
        this.processResult = processResult;
    }

    public void setProcessResultCode(String processResultCode) {
        this.processResultCode = processResultCode;
    }

    public void setCommittee(String committee) {
        this.committee = committee;
    }

}
