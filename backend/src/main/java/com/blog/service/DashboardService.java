package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.DashboardVO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Member;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.DailyStatMapper;
import com.blog.mapper.MemberMapper;
import com.blog.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final ProjectMapper projectMapper;
    private final DailyStatMapper dailyStatMapper;
    private final MemberMapper memberMapper;

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

        vo.setHotArticles(articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT 10")));
        vo.setCategoryViewSums(articleMapper.categoryViewSums());

        Long memberCount = memberMapper.selectCount(null);
        vo.setMemberCount(memberCount == null ? 0 : memberCount);
        LocalDateTime now = LocalDateTime.now();
        Long g7 = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .ge(Member::getCreatedAt, now.minusDays(7)));
        Long g30 = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .ge(Member::getCreatedAt, now.minusDays(30)));
        vo.setMemberGrowth7d(g7 == null ? 0 : g7);
        vo.setMemberGrowth30d(g30 == null ? 0 : g30);
        return vo;
    }
}
