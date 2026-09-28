package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.DailyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface DailyStatMapper extends BaseMapper<DailyStat> {

    @Insert("""
            INSERT INTO daily_stat (stat_date, pv) VALUES (#{date}, 1)
            ON DUPLICATE KEY UPDATE pv = pv + 1
            """)
    int incrPv(@Param("date") LocalDate date);

    @Select("SELECT * FROM daily_stat WHERE stat_date >= #{from} ORDER BY stat_date ASC")
    List<DailyStat> listFrom(@Param("from") LocalDate from);

    @Select("SELECT COALESCE(SUM(pv), 0) FROM daily_stat")
    long sumPv();
}
