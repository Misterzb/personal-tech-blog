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
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagVO> tags;
    /** 同专题上一篇 */
    private ArticleNavItem prev;
    /** 同专题下一篇 */
    private ArticleNavItem next;
    /** 专题内进度：当前序号（从 1） */
    private Integer progressIndex;
    /** 专题内文章总数 */
    private Integer progressTotal;

    @Data
    public static class ArticleNavItem {
        private Long id;
        private String title;
        private String slug;

        public ArticleNavItem() {}

        public ArticleNavItem(Long id, String title, String slug) {
            this.id = id;
            this.title = title;
            this.slug = slug;
        }
    }
}
