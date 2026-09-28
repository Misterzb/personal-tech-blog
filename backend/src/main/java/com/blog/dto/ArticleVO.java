package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String contentMd;
    private String contentHtml;
    private String cover;
    private Integer status;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Long viewCount;
    private String seoTitle;
    private String seoDescription;
    private Boolean isTop;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagVO> tags;
}
