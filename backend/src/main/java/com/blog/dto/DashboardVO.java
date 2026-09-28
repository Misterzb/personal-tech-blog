package com.blog.dto;

import com.blog.entity.Article;
import com.blog.entity.DailyStat;
import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {
    private long articleCount;
    private long publishedCount;
    private long draftCount;
    private long totalPv;
    private long pendingComments;
    private long projectCount;
    private List<Article> recentArticles;
    private List<DailyStat> dailyStats;
}
