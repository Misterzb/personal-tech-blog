package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ArticleSaveRequest {
    private Long id;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String slug;
    private String summary;
    @NotBlank(message = "正文不能为空")
    private String contentMd;
    private String cover;
    /** 0 draft 1 published */
    private Integer status;
    private Long categoryId;
    private String seoTitle;
    private String seoDescription;
    private Boolean isTop;
    private Integer sortOrder;
    private List<Long> tagIds;
}
