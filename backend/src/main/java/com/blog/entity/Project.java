package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String summary;
    private String techStack;
    /** 兼容旧数据；新数据优先用 githubUrl / giteeUrl */
    private String repoUrl;
    private String githubUrl;
    private String giteeUrl;
    private String demoUrl;
    private String cover;
    private Integer sortOrder;
    private Boolean isTop;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
