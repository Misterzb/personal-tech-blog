package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface ArticleMapper extends BaseMapper<Article> {

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrViewCount(@Param("id") Long id);

    @Select("""
            <script>
            SELECT a.* FROM article a
            WHERE a.deleted = 0 AND a.status = 1
            <choose>
              <when test="ftKw != null and ftKw != ''">
                AND MATCH(a.title, a.summary, a.content_md) AGAINST(#{ftKw} IN BOOLEAN MODE)
              </when>
              <otherwise>
                AND (a.title LIKE CONCAT('%',#{likeKw},'%')
                  OR a.summary LIKE CONCAT('%',#{likeKw},'%')
                  OR a.content_md LIKE CONCAT('%',#{likeKw},'%'))
              </otherwise>
            </choose>
            ORDER BY a.published_at DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<Article> search(@Param("ftKw") String ftKw,
                         @Param("likeKw") String likeKw,
                         @Param("offset") long offset,
                         @Param("limit") long limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM article a
            WHERE a.deleted = 0 AND a.status = 1
            <choose>
              <when test="ftKw != null and ftKw != ''">
                AND MATCH(a.title, a.summary, a.content_md) AGAINST(#{ftKw} IN BOOLEAN MODE)
              </when>
              <otherwise>
                AND (a.title LIKE CONCAT('%',#{likeKw},'%')
                  OR a.summary LIKE CONCAT('%',#{likeKw},'%')
                  OR a.content_md LIKE CONCAT('%',#{likeKw},'%'))
              </otherwise>
            </choose>
            </script>
            """)
    long searchCount(@Param("ftKw") String ftKw, @Param("likeKw") String likeKw);

    @Select("""
            SELECT c.id AS categoryId, c.name AS categoryName,
                   COALESCE(SUM(a.view_count), 0) AS viewSum
            FROM category c
            LEFT JOIN article a ON a.category_id = c.id AND a.deleted = 0 AND a.status = 1
            WHERE c.deleted = 0
            GROUP BY c.id, c.name
            ORDER BY viewSum DESC
            """)
    List<Map<String, Object>> categoryViewSums();
}
