package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.dto.TaxonomyStatVO;
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

    @Select("""
            SELECT t.id AS id, t.name AS name, t.slug AS slug,
                   COUNT(DISTINCT CASE WHEN a.deleted = 0 AND a.status = 1 THEN a.id END) AS publishedCount,
                   COUNT(DISTINCT CASE WHEN a.deleted = 0 AND a.status = 0 THEN a.id END) AS draftCount,
                   COUNT(DISTINCT CASE WHEN a.deleted = 0 THEN a.id END) AS totalCount
            FROM tag t
            LEFT JOIN article_tag atg ON atg.tag_id = t.id
            LEFT JOIN article a ON a.id = atg.article_id
            WHERE t.deleted = 0
            GROUP BY t.id, t.name, t.slug
            ORDER BY t.name ASC
            """)
    List<TaxonomyStatVO> selectStats();
}
