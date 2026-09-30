package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ApiResponse;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.ArticleSaveRequest;
import com.blog.dto.ArticleVO;
import com.blog.dto.CommentAdminVO;
import com.blog.dto.DashboardVO;
import com.blog.dto.TaxonomyStatVO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ArticleService articleService;
    private final CommentService commentService;
    private final SiteConfigService siteConfigService;
    private final DashboardService dashboardService;
    private final UploadService uploadService;
    private final MemberService memberService;
    private final AnnouncementService announcementService;
    private final FriendLinkService friendLinkService;
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

    /**
     * 专题/标签用量统计：便于清理「前台无展示数据」的空壳项。
     * empty* = 已发布文章数为 0（含仅有草稿或完全未挂文）。
     */
    @GetMapping("/taxonomy-stats")
    public ApiResponse<Map<String, Object>> taxonomyStats() {
        List<TaxonomyStatVO> categories = categoryMapper.selectStats();
        List<TaxonomyStatVO> tags = tagMapper.selectStats();
        Map<String, Object> map = new HashMap<>();
        map.put("categories", categories);
        map.put("tags", tags);
        map.put("emptyCategories", categories.stream()
                .filter(c -> c.getPublishedCount() <= 0)
                .collect(Collectors.toList()));
        map.put("emptyTags", tags.stream()
                .filter(t -> t.getPublishedCount() <= 0)
                .collect(Collectors.toList()));
        return ApiResponse.ok(map);
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

    @GetMapping("/members")
    public ApiResponse<PageResult<Member>> members(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String kw,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        String q = StringUtils.hasText(kw) ? kw : keyword;
        return ApiResponse.ok(memberService.pageAdmin(page, size, q, status));
    }

    @PatchMapping("/members/{id}/status")
    public ApiResponse<Void> memberStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        memberService.updateStatus(id, status);
        return ApiResponse.ok();
    }

    @GetMapping("/comments")
    public ApiResponse<PageResult<com.blog.dto.CommentAdminVO>> comments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String nickname) {
        return ApiResponse.ok(commentService.pageAdmin(page, size, status, memberId, nickname));
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements() {
        return ApiResponse.ok(announcementService.listAll());
    }

    @PostMapping("/announcements")
    public ApiResponse<Announcement> saveAnnouncement(@RequestBody Announcement announcement) {
        return ApiResponse.ok(announcementService.save(announcement));
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/friend-links")
    public ApiResponse<List<FriendLink>> friendLinks() {
        return ApiResponse.ok(friendLinkService.listAll());
    }

    @PostMapping("/friend-links")
    public ApiResponse<FriendLink> saveFriendLink(@RequestBody FriendLink link) {
        return ApiResponse.ok(friendLinkService.save(link));
    }

    @DeleteMapping("/friend-links/{id}")
    public ApiResponse<Void> deleteFriendLink(@PathVariable Long id) {
        friendLinkService.delete(id);
        return ApiResponse.ok();
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
