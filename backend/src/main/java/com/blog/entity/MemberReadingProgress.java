package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_reading_progress")
public class MemberReadingProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long categoryId;
    private Long articleId;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
