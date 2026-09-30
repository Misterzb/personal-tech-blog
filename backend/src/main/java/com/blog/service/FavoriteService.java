package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.dto.ArticleVO;
import com.blog.entity.Article;
import com.blog.entity.MemberFavorite;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.MemberFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final MemberFavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;

    @Transactional
    public void add(Long memberId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException("文章不存在或未发布");
        }
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<MemberFavorite>()
                .eq(MemberFavorite::getMemberId, memberId)
                .eq(MemberFavorite::getArticleId, articleId));
        if (exists != null && exists > 0) {
            return;
        }
        MemberFavorite fav = new MemberFavorite();
        fav.setMemberId(memberId);
        fav.setArticleId(articleId);
        favoriteMapper.insert(fav);
    }

    @Transactional
    public void remove(Long memberId, Long articleId) {
        favoriteMapper.delete(new LambdaQueryWrapper<MemberFavorite>()
                .eq(MemberFavorite::getMemberId, memberId)
                .eq(MemberFavorite::getArticleId, articleId));
    }

    public List<ArticleVO> list(Long memberId) {
        List<MemberFavorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<MemberFavorite>()
                .eq(MemberFavorite::getMemberId, memberId)
                .orderByDesc(MemberFavorite::getCreatedAt));
        return favs.stream()
                .map(f -> {
                    try {
                        var vo = articleService.getById(f.getArticleId());
                        // 会员收藏列表也做正文脱敏
                        vo.setContentMd(null);
                        vo.setContentHtml(null);
                        return vo;
                    } catch (BusinessException e) {
                        return null;
                    }
                })
                .filter(v -> v != null && Integer.valueOf(1).equals(v.getStatus()))
                .toList();
    }
}
