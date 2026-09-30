package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long memberId;
    /** 回复目标会员（父评论作者） */
    private Long replyToMemberId;
    private String nickname;
    private String email;
    private String avatar;
    private String content;
    /** 0 pending 1 approved 2 rejected */
    private Integer status;
    private String ip;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
