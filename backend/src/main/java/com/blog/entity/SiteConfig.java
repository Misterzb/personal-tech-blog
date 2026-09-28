package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_config")
public class SiteConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String siteName;
    private String siteSubtitle;
    private String icp;
    private String aboutMd;
    private String aboutHtml;
    private String socialLinks;
    private String logo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
