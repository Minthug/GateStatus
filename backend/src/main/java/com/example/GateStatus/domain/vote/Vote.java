package com.example.GateStatus.domain.vote;

import com.example.GateStatus.domain.figure.Figure;
import com.example.GateStatus.domain.proposedBill.ProposedBill;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "figure_id")
    private Figure figure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private ProposedBill bill;

    private LocalDate voteDate;

    @Enumerated(EnumType.STRING)
    private VoteResultType voteResult;

    private String meetingName;

    private String voteTitle;



}
