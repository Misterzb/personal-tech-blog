package com.blog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingProgressRequest {
    @NotNull(message = "专题ID不能为空")
    private Long categoryId;
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
}
