package com.example.GateStatus.domain.issue.service.request;

import java.util.ArrayList;
import java.util.List;

public record UpdateIssueCommand(String title, String content, String thumbnail,
                                 List<String> tags, Boolean isHot) {

    public static UpdateIssueCommand of(String title, String content, String thumbnail, List<String> tags, Boolean isHot) {

        return new UpdateIssueCommand(title, content, thumbnail,
                tags != null ? tags : new ArrayList<>(),
                isHot);
    }
}
