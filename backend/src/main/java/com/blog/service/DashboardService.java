package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.DashboardVO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.DailyStatMapper;
import com.blog.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final ProjectMapper projectMapper;
    private final DailyStatMapper dailyStatMapper;

    public DashboardVO get(int days) {
        DashboardVO vo = new DashboardVO();
        vo.setArticleCount(articleMapper.selectCount(null));
        vo.setPublishedCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)));
        vo.setDraftCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getStatus, 0)));
        vo.setPendingComments(commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, 0)));
        vo.setProjectCount(projectMapper.selectCount(null));
        vo.setTotalPv(dailyStatMapper.sumPv());
        vo.setRecentArticles(articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getUpdatedAt).last("LIMIT 5")));
        vo.setDailyStats(dailyStatMapper.listFrom(LocalDate.now().minusDays(days - 1L)));
        return vo;
    }
}
