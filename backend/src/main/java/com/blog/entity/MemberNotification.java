package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_notification")
public class MemberNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    /** comment_reply / system */
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    /** 0 unread 1 read */
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
