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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Pattern BOOLEAN_SPECIAL = Pattern.compile("[+\\-><()~*\"@\\\\]+");

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public PageResult<ArticleVO> pagePublic(long page, long size, Long categoryId, Long tagId) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .eq(categoryId != null, Article::getCategoryId, categoryId);

        if (tagId != null) {
            List<Long> ids = articleTagMapper.selectArticleIdsByTagId(tagId);
            if (ids.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            qw.in(Article::getId, ids);
        }

        if (categoryId != null || tagId != null) {
            qw.orderByAsc(Article::getSortOrder).orderByAsc(Article::getId);
        } else {
            qw.orderByDesc(Article::getIsTop).orderByDesc(Article::getPublishedAt);
        }

        Page<Article> p = articleMapper.selectPage(new Page<>(page, size), qw);
        List<ArticleVO> vos = p.getRecords().stream().map(a -> toPublicListVO(a)).toList();
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
        return new PageResult<>(p.getRecords().stream().map(a -> toVO(a, false, false, false)).toList(), p.getTotal(), page, size);
    }

    public PageResult<ArticleVO> search(String keyword, long page, long size) {
        if (!StringUtils.hasText(keyword)) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        String likeKw = keyword.trim();
        String ftKw = sanitizeBooleanMode(likeKw);
        long offset = (page - 1) * size;
        List<Article> list;
        long total;
        try {
            list = articleMapper.search(ftKw, likeKw, offset, size);
            total = articleMapper.searchCount(ftKw, likeKw);
            // ngram/BOOLEAN 对部分英文短词可能抽不到，回退 LIKE
            if (total == 0 && StringUtils.hasText(ftKw)) {
                list = articleMapper.search(null, likeKw, offset, size);
                total = articleMapper.searchCount(null, likeKw);
            }
        } catch (Exception e) {
            list = articleMapper.search(null, likeKw, offset, size);
            total = articleMapper.searchCount(null, likeKw);
        }
        return new PageResult<>(list.stream().map(this::toPublicListVO).toList(), total, page, size);
    }

    /** 清洗 BOOLEAN MODE 特殊字符；过短则返回空以触发 LIKE 回退 */
    public static String sanitizeBooleanMode(String kw) {
        if (!StringUtils.hasText(kw)) {
            return "";
        }
        String cleaned = BOOLEAN_SPECIAL.matcher(kw.trim()).replaceAll(" ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() < 2) {
            return "";
        }
        // 多词 AND：word*
        return Arrays.stream(cleaned.split(" "))
                .filter(StringUtils::hasText)
                .map(w -> w + "*")
                .collect(Collectors.joining(" "));
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
        return toVO(article, true, true, false);
    }

    public ArticleVO getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return toVO(article, false, false, false);
    }

    /** 专题目录：按 sort_order,id 有序文章列表 */
    public List<Map<String, Object>> categoryToc(String idOrSlug) {
        Category category = resolveCategory(idOrSlug);
        if (category == null) {
            throw new BusinessException("专题不存在");
        }
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, category.getId())
                .eq(Article::getStatus, 1)
                .orderByAsc(Article::getSortOrder)
                .orderByAsc(Article::getId));
        List<Map<String, Object>> toc = new ArrayList<>();
        int i = 1;
        for (Article a : articles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("slug", a.getSlug());
            item.put("sortOrder", a.getSortOrder());
            item.put("index", i++);
            item.put("summary", a.getSummary());
            toc.add(item);
        }
        return toc;
    }

    private Category resolveCategory(String idOrSlug) {
        if (!StringUtils.hasText(idOrSlug)) {
            return null;
        }
        if (idOrSlug.matches("^\\d+$")) {
            return categoryMapper.selectById(Long.parseLong(idOrSlug));
        }
        return categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getSlug, idOrSlug));
    }

    @Transactional
    @CacheEvict(cacheNames = {"home", "categories", "tags"}, allEntries = true)
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
        article.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
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
        return toVO(article, false, false, false);
    }

    @CacheEvict(cacheNames = {"home", "categories", "tags"}, allEntries = true)
    public void delete(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            String freed = (article.getSlug() + "-deleted-" + id);
            if (freed.length() > 220) {
                freed = freed.substring(0, 220);
            }
            article.setSlug(freed);
            articleMapper.updateById(article);
        }
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

    /** 前台列表：不返回正文，降低批量爬取成本 */
    private ArticleVO toPublicListVO(Article article) {
        return toVO(article, false, true, true);
    }

    /**
     * @param publicView 前台接口：去掉 Markdown 原文
     * @param listView   列表：再去掉 HTML 正文（详情页保留 contentHtml）
     */
    private ArticleVO toVO(Article article, boolean withSeriesNav, boolean publicView, boolean listView) {
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
        if (withSeriesNav && article.getCategoryId() != null) {
            fillSeriesNav(vo, article);
        }
        if (publicView) {
            vo.setContentMd(null);
            if (listView) {
                vo.setContentHtml(null);
            }
        }
        return vo;
    }

    private void fillSeriesNav(ArticleVO vo, Article article) {
        List<Article> series = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, article.getCategoryId())
                .eq(Article::getStatus, 1)
                .orderByAsc(Article::getSortOrder)
                .orderByAsc(Article::getId)
                .select(Article::getId, Article::getTitle, Article::getSlug, Article::getSortOrder));
        vo.setProgressTotal(series.size());
        int idx = -1;
        for (int i = 0; i < series.size(); i++) {
            if (series.get(i).getId().equals(article.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            vo.setProgressIndex(idx + 1);
            if (idx > 0) {
                Article prev = series.get(idx - 1);
                vo.setPrev(new ArticleVO.ArticleNavItem(prev.getId(), prev.getTitle(), prev.getSlug()));
            }
            if (idx < series.size() - 1) {
                Article next = series.get(idx + 1);
                vo.setNext(new ArticleVO.ArticleNavItem(next.getId(), next.getTitle(), next.getSlug()));
            }
        }
    }
}
