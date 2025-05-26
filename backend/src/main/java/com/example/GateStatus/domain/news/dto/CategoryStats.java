package com.example.GateStatus.domain.news.dto;

import com.example.GateStatus.domain.news.NewsDocument;

public class CategoryStats {
    private final String category;
    private int count = 0;
    private int totalViews = 0;
    private int totalComments = 0;

    public CategoryStats(String category) {
        this.category = category;
    }

    public void addNews(NewsDocument news) {
        count++;
        if (news.getViewCount() != null) {
            totalViews += news.getViewCount();
        }

        if (news.getCommentCount() != null) {
            totalComments += news.getCommentCount();
        }
    }

    public double getAverageViews() {
        return count > 0 ? (double) totalViews / count : 0;
    }

    public double getAverageComments() {
        return  count > 0 ? (double) totalComments / count : 0;
    }
}
