package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.ArticleSaveRequest;
import com.blog.dto.ArticleVO;
import com.blog.dto.TagVO;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.util.MarkdownUtil;
import com.blog.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public PageResult<ArticleVO> pagePublic(long page, long size, Long categoryId, Long tagId) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getPublishedAt);

        if (tagId != null) {
            List<Long> ids = articleTagMapper.selectArticleIdsByTagId(tagId);
            if (ids.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            qw.in(Article::getId, ids);
        }

        Page<Article> p = articleMapper.selectPage(new Page<>(page, size), qw);
        List<ArticleVO> vos = p.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(vos, p.getTotal(), page, size);
    }

    public PageResult<ArticleVO> pageAdmin(long page, long size, Integer status, String keyword) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>()
                .eq(status != null, Article::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword))
                .orderByDesc(Article::getUpdatedAt);
        Page<Article> p = articleMapper.selectPage(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords().stream().map(this::toVO).toList(), p.getTotal(), page, size);
    }

    public PageResult<ArticleVO> search(String keyword, long page, long size) {
        if (!StringUtils.hasText(keyword)) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<Article> list = articleMapper.search(keyword.trim(), offset, size);
        long total = articleMapper.searchCount(keyword.trim());
        return new PageResult<>(list.stream().map(this::toVO).toList(), total, page, size);
    }

    public ArticleVO getBySlug(String slug, boolean incrView) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug)
                .eq(Article::getStatus, 1));
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (incrView) {
            articleMapper.incrViewCount(article.getId());
            article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
        }
        return toVO(article);
    }

    public ArticleVO getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return toVO(article);
    }

    @Transactional
    public ArticleVO save(ArticleSaveRequest req) {
        Article article = req.getId() != null ? articleMapper.selectById(req.getId()) : new Article();
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setTitle(req.getTitle());
        String slug = StringUtils.hasText(req.getSlug()) ? req.getSlug() : SlugUtil.toSlug(req.getTitle());
        slug = ensureUniqueSlug(slug, article.getId());
        article.setSlug(slug);
        article.setSummary(req.getSummary());
        article.setContentMd(req.getContentMd());
        article.setContentHtml(MarkdownUtil.toHtml(req.getContentMd()));
        article.setCover(req.getCover());
        article.setStatus(req.getStatus() == null ? 0 : req.getStatus());
        article.setCategoryId(req.getCategoryId());
        article.setSeoTitle(req.getSeoTitle());
        article.setSeoDescription(req.getSeoDescription());
        article.setIsTop(Boolean.TRUE.equals(req.getIsTop()));
        if (article.getViewCount() == null) {
            article.setViewCount(0L);
        }
        if (Objects.equals(article.getStatus(), 1) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        if (article.getId() == null) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
            articleTagMapper.deleteByArticleId(article.getId());
        }
        if (!CollectionUtils.isEmpty(req.getTagIds())) {
            for (Long tagId : req.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }
        return toVO(article);
    }

    public void delete(Long id) {
        articleMapper.deleteById(id);
        articleTagMapper.deleteByArticleId(id);
    }

    public List<Article> listPublishedForFeed(int limit) {
        return articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getPublishedAt)
                .last("LIMIT " + limit));
    }

    private String ensureUniqueSlug(String slug, Long excludeId) {
        String base = slug;
        int i = 1;
        while (true) {
            Article exist = articleMapper.selectOne(new LambdaQueryWrapper<Article>().eq(Article::getSlug, slug));
            if (exist == null || (excludeId != null && excludeId.equals(exist.getId()))) {
                return slug;
            }
            slug = base + "-" + i++;
        }
    }

    private ArticleVO toVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        if (article.getCategoryId() != null) {
            Category c = categoryMapper.selectById(article.getCategoryId());
            if (c != null) {
                vo.setCategoryName(c.getName());
                vo.setCategorySlug(c.getSlug());
            }
        }
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(article.getId());
        if (!tagIds.isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            vo.setTags(tags.stream().map(t -> new TagVO(t.getId(), t.getName(), t.getSlug())).collect(Collectors.toList()));
        } else {
            vo.setTags(List.of());
        }
        return vo;
    }
}
