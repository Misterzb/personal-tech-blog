package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrViewCount(@Param("id") Long id);

    @Select("""
            SELECT DISTINCT a.* FROM article a
            LEFT JOIN article_tag at ON a.id = at.article_id
            LEFT JOIN tag t ON at.tag_id = t.id
            WHERE a.deleted = 0 AND a.status = 1
              AND (a.title LIKE CONCAT('%',#{kw},'%')
                OR a.summary LIKE CONCAT('%',#{kw},'%')
                OR a.content_md LIKE CONCAT('%',#{kw},'%')
                OR t.name LIKE CONCAT('%',#{kw},'%'))
            ORDER BY a.published_at DESC
            LIMIT #{offset}, #{limit}
            """)
    List<Article> search(@Param("kw") String kw, @Param("offset") long offset, @Param("limit") long limit);

    @Select("""
            SELECT COUNT(DISTINCT a.id) FROM article a
            LEFT JOIN article_tag at ON a.id = at.article_id
            LEFT JOIN tag t ON at.tag_id = t.id
            WHERE a.deleted = 0 AND a.status = 1
              AND (a.title LIKE CONCAT('%',#{kw},'%')
                OR a.summary LIKE CONCAT('%',#{kw},'%')
                OR a.content_md LIKE CONCAT('%',#{kw},'%')
                OR t.name LIKE CONCAT('%',#{kw},'%'))
            """)
    long searchCount(@Param("kw") String kw);
}
