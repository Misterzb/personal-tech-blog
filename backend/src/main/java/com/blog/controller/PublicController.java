package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.config.BlogProperties;
import com.blog.dto.ArticleVO;
import com.blog.dto.CommentRequest;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.Project;
import com.blog.entity.SiteConfig;
import com.blog.entity.Tag;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.ProjectMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.ArticleService;
import com.blog.service.CommentService;
import com.blog.service.SiteConfigService;
import com.blog.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicController {

    private final ArticleService articleService;
    private final CommentService commentService;
    private final SiteConfigService siteConfigService;
    private final VisitService visitService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ProjectMapper projectMapper;
    private final BlogProperties blogProperties;

    @GetMapping("/api/public/site")
    public ApiResponse<SiteConfig> site() {
        return ApiResponse.ok(siteConfigService.get());
    }

    @GetMapping("/api/public/articles")
    public ApiResponse<PageResult<ArticleVO>> articles(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        return ApiResponse.ok(articleService.pagePublic(page, size, categoryId, tagId));
    }

    @GetMapping("/api/public/articles/{slug}")
    public ApiResponse<ArticleVO> articleDetail(@PathVariable String slug) {
        return ApiResponse.ok(articleService.getBySlug(slug, true));
    }

    @GetMapping("/api/public/search")
    public ApiResponse<PageResult<ArticleVO>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(articleService.search(q, page, size));
    }

    @GetMapping("/api/public/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder).orderByAsc(Category::getId)));
    }

    @GetMapping("/api/public/categories/{slug}")
    public ApiResponse<Category> category(@PathVariable String slug) {
        Category c = categoryMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
        return ApiResponse.ok(c);
    }

    @GetMapping("/api/public/tags")
    public ApiResponse<List<Tag>> tags() {
        // 前台只展示「至少挂在一篇已发布文章上」的标签，避免空壳标签误导筛选
        return ApiResponse.ok(tagMapper.selectUsedByPublishedArticles());
    }

    @GetMapping("/api/public/projects")
    public ApiResponse<List<Project>> projects() {
        return ApiResponse.ok(projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getIsTop).orderByAsc(Project::getSortOrder).orderByDesc(Project::getId)));
    }

    @GetMapping("/api/public/comments")
    public ApiResponse<PageResult<Comment>> comments(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        return ApiResponse.ok(commentService.listApproved(articleId, page, size));
    }

    @PostMapping("/api/public/comments")
    public ApiResponse<Void> submitComment(@Valid @RequestBody CommentRequest req, HttpServletRequest request) {
        commentService.submit(req, clientIp(request));
        return ApiResponse.ok();
    }

    @PostMapping("/api/public/visit")
    public ApiResponse<Void> visit(@RequestBody Map<String, String> body, HttpServletRequest request) {
        visitService.track(body.getOrDefault("path", "/"), clientIp(request), request.getHeader("User-Agent"));
        return ApiResponse.ok();
    }

    @GetMapping("/api/public/home")
    public ApiResponse<Map<String, Object>> home() {
        Map<String, Object> map = new HashMap<>();
        map.put("site", siteConfigService.get());
        map.put("articles", articleService.pagePublic(1, 6, null, null).getRecords());
        map.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder).last("LIMIT 6")));
        map.put("projects", projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .orderByDesc(Project::getIsTop).orderByAsc(Project::getSortOrder).last("LIMIT 4")));
        return ApiResponse.ok(map);
    }

    @GetMapping(value = "/rss.xml", produces = MediaType.APPLICATION_RSS_XML_VALUE)
    public String rss() {
        String base = blogProperties.getSite().getBaseUrl();
        SiteConfig site = siteConfigService.get();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\"><channel>");
        sb.append("<title>").append(esc(site.getSiteName())).append("</title>");
        sb.append("<link>").append(esc(base)).append("</link>");
        sb.append("<description>").append(esc(site.getSiteSubtitle())).append("</description>");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        articleService.listPublishedForFeed(20).forEach(a -> {
            sb.append("<item>");
            sb.append("<title>").append(esc(a.getTitle())).append("</title>");
            sb.append("<link>").append(esc(base + "/articles/" + a.getSlug())).append("</link>");
            sb.append("<guid>").append(esc(base + "/articles/" + a.getSlug())).append("</guid>");
            if (a.getSummary() != null) {
                sb.append("<description>").append(esc(a.getSummary())).append("</description>");
            }
            if (a.getPublishedAt() != null) {
                sb.append("<pubDate>").append(a.getPublishedAt().format(fmt)).append("</pubDate>");
            }
            sb.append("</item>");
        });
        sb.append("</channel></rss>");
        return sb.toString();
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String base = blogProperties.getSite().getBaseUrl();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (String path : List.of("/", "/articles", "/categories", "/projects", "/about")) {
            sb.append("<url><loc>").append(esc(base + path)).append("</loc></url>");
        }
        articleService.listPublishedForFeed(200).forEach(a ->
                sb.append("<url><loc>").append(esc(base + "/articles/" + a.getSlug())).append("</loc></url>"));
        sb.append("</urlset>");
        return sb.toString();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        String base = blogProperties.getSite().getBaseUrl();
        return "User-agent: *\nAllow: /\nSitemap: " + base + "/sitemap.xml\n";
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
