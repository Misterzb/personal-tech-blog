package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    private Long parentId;
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称过长")
    private String nickname;
    private String email;
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论过长")
    private String content;
}
