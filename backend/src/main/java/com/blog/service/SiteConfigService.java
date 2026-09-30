package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import com.blog.util.MarkdownUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigMapper siteConfigMapper;

    @Cacheable(cacheNames = "site", key = "'default'")
    public SiteConfig get() {
        SiteConfig config = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>().last("LIMIT 1"));
        if (config == null) {
            config = new SiteConfig();
            config.setSiteName("技术实践笔记");
            config.setSiteSubtitle("Java · Agent · 全栈落地");
            config.setAboutMd("你好，我是一名技术人员。");
            config.setAboutHtml(MarkdownUtil.toHtml(config.getAboutMd()));
            config.setSocialLinks("[]");
            siteConfigMapper.insert(config);
        }
        return config;
    }

    @CacheEvict(cacheNames = {"site", "home"}, allEntries = true)
    public SiteConfig save(SiteConfig incoming) {
        SiteConfig config = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>().last("LIMIT 1"));
        if (config == null) {
            config = new SiteConfig();
            siteConfigMapper.insert(config);
            config = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfig>().last("LIMIT 1"));
        }
        config.setSiteName(incoming.getSiteName());
        config.setSiteSubtitle(incoming.getSiteSubtitle());
        config.setIcp(incoming.getIcp());
        config.setAboutMd(incoming.getAboutMd());
        config.setAboutHtml(MarkdownUtil.toHtml(incoming.getAboutMd()));
        config.setSocialLinks(incoming.getSocialLinks());
        config.setLogo(incoming.getLogo());
        siteConfigMapper.updateById(config);
        return config;
    }
}
