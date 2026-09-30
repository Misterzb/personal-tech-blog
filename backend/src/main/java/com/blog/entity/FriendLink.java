package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_link")
public class FriendLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String url;
    private String logo;
    private String description;
    /** 0 inactive 1 active */
    private Integer status;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
