package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.dto.ReadingProgressRequest;
import com.blog.entity.Article;
import com.blog.entity.MemberReadingProgress;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.MemberReadingProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingProgressService {

    private final MemberReadingProgressMapper progressMapper;
    private final ArticleMapper articleMapper;

    @Transactional
    public Map<String, Object> save(Long memberId, ReadingProgressRequest req) {
        Article article = articleMapper.selectById(req.getArticleId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException("文章不存在或未发布");
        }
        if (!req.getCategoryId().equals(article.getCategoryId())) {
            throw new BusinessException("文章不属于该专题");
        }
        MemberReadingProgress exist = progressMapper.selectOne(new LambdaQueryWrapper<MemberReadingProgress>()
                .eq(MemberReadingProgress::getMemberId, memberId)
                .eq(MemberReadingProgress::getCategoryId, req.getCategoryId()));
        if (exist == null) {
            exist = new MemberReadingProgress();
            exist.setMemberId(memberId);
            exist.setCategoryId(req.getCategoryId());
            exist.setArticleId(req.getArticleId());
            exist.setUpdatedAt(LocalDateTime.now());
            progressMapper.insert(exist);
        } else {
            exist.setArticleId(req.getArticleId());
            exist.setUpdatedAt(LocalDateTime.now());
            progressMapper.updateById(exist);
        }
        return toMap(exist);
    }

    public Map<String, Object> get(Long memberId, Long categoryId) {
        MemberReadingProgress exist = progressMapper.selectOne(new LambdaQueryWrapper<MemberReadingProgress>()
                .eq(MemberReadingProgress::getMemberId, memberId)
                .eq(MemberReadingProgress::getCategoryId, categoryId));
        if (exist == null) {
            return Map.of();
        }
        return toMap(exist);
    }

    private Map<String, Object> toMap(MemberReadingProgress p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("memberId", p.getMemberId());
        map.put("categoryId", p.getCategoryId());
        map.put("articleId", p.getArticleId());
        map.put("updatedAt", p.getUpdatedAt());
        Article a = articleMapper.selectById(p.getArticleId());
        if (a != null) {
            map.put("articleTitle", a.getTitle());
            map.put("articleSlug", a.getSlug());
        }
        return map;
    }
}
