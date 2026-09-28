package com.blog.config;

import com.blog.entity.*;
import com.blog.mapper.*;
import com.blog.util.MarkdownUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ProjectMapper projectMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureAdmin();
        ensureSiteConfig();
        if (categoryMapper.selectCount(null) == 0) {
            seedContent();
            log.info("Seed data initialized. Default admin: admin / admin123");
        }
    }

    private void ensureAdmin() {
        Long count = userMapper.selectCount(null);
        if (count == 0) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("admin123"));
            user.setNickname("站长");
            userMapper.insert(user);
        }
    }

    private void ensureSiteConfig() {
        if (siteConfigMapper.selectCount(null) == 0) {
            SiteConfig config = new SiteConfig();
            config.setSiteName("技术实践笔记");
            config.setSiteSubtitle("Java · Agent · 全栈落地");
            config.setIcp("");
            String about = """
                    ## 关于我

                    我是一名技术人员，长期关注工程落地与产品可用性。

                    **擅长方向**

                    - Java 后端与工程实践
                    - Agent 落地与应用开发
                    - Python 工具与自动化
                    - 小程序 / 公众号
                    - Web 网站开发

                    欢迎交流，也欢迎关注本站开源项目。
                    """;
            config.setAboutMd(about);
            config.setAboutHtml(MarkdownUtil.toHtml(about));
            config.setSocialLinks("[{\"name\":\"GitHub\",\"url\":\"https://github.com\"},{\"name\":\"Gitee\",\"url\":\"https://gitee.com\"}]");
            siteConfigMapper.insert(config);
        }
    }

    private void seedContent() {
        List<Category> categories = List.of(
                cat("Java 工程实践", "java", "Spring Boot、工程化与性能优化", 1),
                cat("Agent 落地与应用", "agent", "大模型 Agent 架构与业务落地", 2),
                cat("Python 工具与自动化", "python", "脚本、爬虫与效率工具", 3),
                cat("小程序 / 公众号", "miniapp", "微信生态产品开发经验", 4),
                cat("Web 全栈", "web", "前端与全栈网站实践", 5),
                cat("随笔与职场", "notes", "学习方法与成长记录", 6)
        );
        categories.forEach(categoryMapper::insert);

        Tag spring = tag("Spring Boot", "spring-boot");
        Tag agent = tag("Agent", "agent");
        Tag vue = tag("Vue", "vue");
        tagMapper.insert(spring);
        tagMapper.insert(agent);
        tagMapper.insert(vue);

        String md = """
                # 欢迎来到技术实践笔记

                这是一篇示例文章，用于演示博客的 Markdown 渲染、专题与标签能力。

                ## 你可以在这里写什么

                - Java / Spring Boot 工程实践
                - Agent 落地踩坑与复盘
                - 小程序、公众号与 Web 全栈经验

                ```java
                @RestController
                public class HelloController {
                    @GetMapping("/hello")
                    public String hello() {
                        return "Hello Blog";
                    }
                }
                ```

                登录管理后台即可发布你的第一篇正式文章。
                """;
        Article a1 = new Article();
        a1.setTitle("欢迎使用技术实践笔记");
        a1.setSlug("welcome");
        a1.setSummary("站点已就绪：前台阅读、后台发布、专题与开源项目展示。");
        a1.setContentMd(md);
        a1.setContentHtml(MarkdownUtil.toHtml(md));
        a1.setStatus(1);
        a1.setCategoryId(categories.get(0).getId());
        a1.setViewCount(0L);
        a1.setIsTop(true);
        a1.setPublishedAt(LocalDateTime.now());
        a1.setSeoTitle("欢迎使用技术实践笔记");
        a1.setSeoDescription(a1.getSummary());
        articleMapper.insert(a1);

        ArticleTag at1 = new ArticleTag();
        at1.setArticleId(a1.getId());
        at1.setTagId(spring.getId());
        articleTagMapper.insert(at1);

        String md2 = """
                # Agent 落地：从 Demo 到可用系统

                Agent 不只是提示词工程。真正落地需要关注：

                1. 工具调用边界与权限
                2. 会话状态与可观测性
                3. 失败重试与人工接管

                本文作为专题占位，后续可持续补充实战案例。
                """;
        Article a2 = new Article();
        a2.setTitle("Agent 落地：从 Demo 到可用系统");
        a2.setSlug("agent-from-demo-to-production");
        a2.setSummary("把 Agent 从演示环境推进到可维护、可观测的业务系统。");
        a2.setContentMd(md2);
        a2.setContentHtml(MarkdownUtil.toHtml(md2));
        a2.setStatus(1);
        a2.setCategoryId(categories.get(1).getId());
        a2.setViewCount(0L);
        a2.setIsTop(false);
        a2.setPublishedAt(LocalDateTime.now().minusDays(1));
        articleMapper.insert(a2);
        ArticleTag at2 = new ArticleTag();
        at2.setArticleId(a2.getId());
        at2.setTagId(agent.getId());
        articleTagMapper.insert(at2);

        Project p1 = new Project();
        p1.setName("个人技术博客");
        p1.setSummary("Spring Boot + Vue 3 前后端分离个人博客，含后台发布、专题与开源展示。");
        p1.setTechStack("Java,Spring Boot,Vue 3,MySQL");
        p1.setGithubUrl("https://github.com/Misterzb/personal-tech-blog.git");
        p1.setGiteeUrl("https://gitee.com/bo_live/personal-tech-blog.git");
        p1.setRepoUrl("https://github.com/Misterzb/personal-tech-blog.git");
        p1.setDemoUrl("/");
        p1.setSortOrder(1);
        p1.setIsTop(true);
        projectMapper.insert(p1);

        Project p2 = new Project();
        p2.setName("Agent 工具箱");
        p2.setSummary("面向业务场景的 Agent 编排与工具调用示例集合。");
        p2.setTechStack("Python,LangChain,FastAPI");
        p2.setGithubUrl("");
        p2.setGiteeUrl("");
        p2.setRepoUrl("");
        p2.setSortOrder(2);
        p2.setIsTop(false);
        projectMapper.insert(p2);
    }

    private Category cat(String name, String slug, String desc, int sort) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setDescription(desc);
        c.setSortOrder(sort);
        return c;
    }

    private Tag tag(String name, String slug) {
        Tag t = new Tag();
        t.setName(name);
        t.setSlug(slug);
        return t;
    }
}
