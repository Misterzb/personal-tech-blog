package com.blog.dto;

import lombok.Data;

@Data
public class TaxonomyStatVO {
    private Long id;
    private String name;
    private String slug;
    /** 已发布文章数（前台可见） */
    private long publishedCount;
    /** 草稿文章数 */
    private long draftCount;
    /** 全部未删除文章数（发布+草稿） */
    private long totalCount;
}
