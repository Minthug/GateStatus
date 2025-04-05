package com.example.GateStatus.domain.issue.repository.request;

import java.util.ArrayList;
import java.util.List;

public record UpdateIssueCommand(String title, String content, String thumbnail,
                                 List<String> tags, int viewCount, Boolean isHot) {

    public static UpdateIssueCommand of(String title, String content, String thumbnail, List<String> tags, int viewCount, Boolean isHot) {

        return new UpdateIssueCommand(title, content, thumbnail,
                tags != null ? tags : new ArrayList<>(),
                viewCount,
                isHot);
    }
}
