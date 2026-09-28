package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ApiResponse;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.ArticleSaveRequest;
import com.blog.dto.ArticleVO;
import com.blog.dto.DashboardVO;
import com.blog.entity.*;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.ProjectMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.*;
import com.blog.util.SlugUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ArticleService articleService;
    private final CommentService commentService;
    private final SiteConfigService siteConfigService;
    private final DashboardService dashboardService;
    private final UploadService uploadService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ProjectMapper projectMapper;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardVO> dashboard(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(dashboardService.get(days));
    }

    @GetMapping("/articles")
    public ApiResponse<PageResult<ArticleVO>> articles(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(articleService.pageAdmin(page, size, status, keyword));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<ArticleVO> article(@PathVariable Long id) {
        return ApiResponse.ok(articleService.getById(id));
    }

    @PostMapping("/articles")
    public ApiResponse<ArticleVO> saveArticle(@Valid @RequestBody ArticleSaveRequest req) {
        return ApiResponse.ok(articleService.save(req));
    }

    @DeleteMapping("/articles/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)));
    }

    @PostMapping("/categories")
    public ApiResponse<Category> saveCategory(@RequestBody Category category) {
        if (!StringUtils.hasText(category.getSlug())) {
            category.setSlug(SlugUtil.toSlug(category.getName()));
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return ApiResponse.ok(category);
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags() {
        return ApiResponse.ok(tagMapper.selectList(null));
    }

    @PostMapping("/tags")
    public ApiResponse<Tag> saveTag(@RequestBody Tag tag) {
        if (!StringUtils.hasText(tag.getSlug())) {
            tag.setSlug(SlugUtil.toSlug(tag.getName()));
        }
        if (tag.getId() == null) {
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        return ApiResponse.ok(tag);
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        articleTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/projects")
    public ApiResponse<List<Project>> projects() {
        return ApiResponse.ok(projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByAsc(Project::getSortOrder)));
    }

    @PostMapping("/projects")
    public ApiResponse<Project> saveProject(@RequestBody Project project) {
        if (project.getSortOrder() == null) {
            project.setSortOrder(0);
        }
        if (project.getIsTop() == null) {
            project.setIsTop(false);
        }
        // 兼容旧字段：优先同步 github，否则 gitee
        if (!StringUtils.hasText(project.getRepoUrl())) {
            if (StringUtils.hasText(project.getGithubUrl())) {
                project.setRepoUrl(project.getGithubUrl());
            } else if (StringUtils.hasText(project.getGiteeUrl())) {
                project.setRepoUrl(project.getGiteeUrl());
            }
        }
        if (project.getId() == null) {
            projectMapper.insert(project);
        } else {
            projectMapper.updateById(project);
        }
        return ApiResponse.ok(project);
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/comments")
    public ApiResponse<PageResult<Comment>> comments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(commentService.pageAdmin(page, size, status));
    }

    @PutMapping("/comments/{id}/status")
    public ApiResponse<Void> commentStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            throw new BusinessException("status 不能为空");
        }
        commentService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/site")
    public ApiResponse<SiteConfig> getSite() {
        return ApiResponse.ok(siteConfigService.get());
    }

    @PostMapping("/site")
    public ApiResponse<SiteConfig> saveSite(@RequestBody SiteConfig config) {
        return ApiResponse.ok(siteConfigService.save(config));
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = uploadService.upload(file);
        return ApiResponse.ok(Map.of("url", url));
    }
}
