package com.blog.dto;

import com.blog.entity.Article;
import com.blog.entity.DailyStat;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
    /** 按阅读量 Top10 */
    private List<Article> hotArticles;
    /** 专题阅读量汇总 [{categoryId,categoryName,viewSum}] */
    private List<Map<String, Object>> categoryViewSums;
    private long memberCount;
    private long memberGrowth7d;
    private long memberGrowth30d;
}
