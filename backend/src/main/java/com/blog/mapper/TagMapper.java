package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TagMapper extends BaseMapper<Tag> {

    @Select("""
            SELECT DISTINCT t.id, t.name, t.slug, t.created_at, t.updated_at, t.deleted
            FROM tag t
            INNER JOIN article_tag at ON at.tag_id = t.id
            INNER JOIN article a ON a.id = at.article_id
            WHERE t.deleted = 0 AND a.deleted = 0 AND a.status = 1
            ORDER BY t.name ASC
            """)
    List<Tag> selectUsedByPublishedArticles();
}
