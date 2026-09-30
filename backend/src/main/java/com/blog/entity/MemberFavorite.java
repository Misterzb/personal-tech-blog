package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_favorite")
public class MemberFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long articleId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
