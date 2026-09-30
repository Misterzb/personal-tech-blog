package com.blog.dto;

import lombok.Data;

@Data
public class CommentAdminVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long memberId;
    private Long replyToMemberId;
    private String nickname;
    private String email;
    private String avatar;
    private String content;
    private Integer status;
    private String ip;
    private String memberPhone;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
