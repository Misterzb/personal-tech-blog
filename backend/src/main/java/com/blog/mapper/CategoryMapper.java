package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.dto.TaxonomyStatVO;
import com.blog.entity.Category;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {

    @Select("""
            SELECT DISTINCT c.id, c.name, c.slug, c.description, c.cover, c.sort_order,
                   c.created_at, c.updated_at, c.deleted
            FROM category c
            INNER JOIN article a ON a.category_id = c.id
            WHERE c.deleted = 0 AND a.deleted = 0 AND a.status = 1
            ORDER BY c.sort_order ASC, c.id ASC
            """)
    List<Category> selectUsedByPublishedArticles();

    @Select("""
            SELECT c.id AS id, c.name AS name, c.slug AS slug,
                   COALESCE(SUM(CASE WHEN a.deleted = 0 AND a.status = 1 THEN 1 ELSE 0 END), 0) AS publishedCount,
                   COALESCE(SUM(CASE WHEN a.deleted = 0 AND a.status = 0 THEN 1 ELSE 0 END), 0) AS draftCount,
                   COALESCE(SUM(CASE WHEN a.deleted = 0 THEN 1 ELSE 0 END), 0) AS totalCount
            FROM category c
            LEFT JOIN article a ON a.category_id = c.id
            WHERE c.deleted = 0
            GROUP BY c.id, c.name, c.slug, c.sort_order
            ORDER BY c.sort_order ASC, c.id ASC
            """)
    List<TaxonomyStatVO> selectStats();
}
