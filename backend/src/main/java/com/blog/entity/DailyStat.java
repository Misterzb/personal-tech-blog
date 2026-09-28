package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("daily_stat")
public class DailyStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long pv;
}
