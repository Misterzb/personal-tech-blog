package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_category_sub")
public class MemberCategorySub {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private Long categoryId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
