package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.SiteConfig;
import com.blog.mapper.SiteConfigMapper;
import com.blog.util.MarkdownUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigMapper siteConfigMapper;

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

    public SiteConfig save(SiteConfig incoming) {
        SiteConfig config = get();
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
