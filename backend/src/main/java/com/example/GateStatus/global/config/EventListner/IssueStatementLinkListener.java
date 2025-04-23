package com.example.GateStatus.global.config.EventListner;

import com.example.GateStatus.domain.statement.mongo.StatementDocument;
import com.example.GateStatus.domain.statement.repository.StatementMongoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class IssueStatementLinkListener {

    private final StatementMongoRepository statementMongoRepository;

    @EventListener
    public void handleIssueLinkedToStatement(IssueLinkedToStatementEvent event) {
        String issueId = event.getIssueId();
        String statementId = event.getStatementId();

        StatementDocument statement = statementMongoRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException("해당 발언을 찾을 수 없습니다" + statementId));

        if (statement.getIssueIds() == null) {
            statement.setIssueIds(new ArrayList<>());
        }

        if (!statement.getIssueIds().contains(issueId)) {
            statement.getIssueIds().add(issueId);
            statementMongoRepository.save(statement);
        }
    }
}