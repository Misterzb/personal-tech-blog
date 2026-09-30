package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String contentMd;
    private String contentHtml;
    private String cover;
    /** 0 draft 1 published */
    private Integer status;
    private Long categoryId;
    private Long viewCount;
    private String seoTitle;
    private String seoDescription;
    private Boolean isTop;
    /** 专题内排序，越小越靠前；0 表示未指定 */
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
