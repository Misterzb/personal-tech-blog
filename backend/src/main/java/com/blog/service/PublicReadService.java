package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ArticleVO;
import com.blog.entity.Category;
import com.blog.entity.Project;
import com.blog.entity.SiteConfig;
import com.blog.entity.Tag;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.ProjectMapper;
import com.blog.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台只读聚合，在 service 层缓存（可正确序列化），避免缓存 ApiResponse。
 */
@Service
@RequiredArgsConstructor
public class PublicReadService {

    private final SiteConfigService siteConfigService;
    private final ArticleService articleService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ProjectMapper projectMapper;

    @Cacheable(cacheNames = "home", key = "'v1'")
    public Map<String, Object> home() {
        Map<String, Object> map = new HashMap<>();
        map.put("site", siteConfigService.get());
        map.put("articles", articleService.pagePublic(1, 6, null, null).getRecords());
        map.put("categories", categoryMapper.selectUsedByPublishedArticles().stream().limit(6).toList());
        map.put("projects", projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getIsTop).orderByAsc(Project::getSortOrder).last("LIMIT 4")));
        return map;
    }

    @Cacheable(cacheNames = "categories", key = "'used'")
    public List<Category> categoriesUsed() {
        return categoryMapper.selectUsedByPublishedArticles();
    }

    @Cacheable(cacheNames = "tags", key = "'used'")
    public List<Tag> tagsUsed() {
        return tagMapper.selectUsedByPublishedArticles();
    }
}
