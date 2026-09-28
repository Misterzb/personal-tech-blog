package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String path;
    private String ipMask;
    private String userAgent;
    private LocalDate visitDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
