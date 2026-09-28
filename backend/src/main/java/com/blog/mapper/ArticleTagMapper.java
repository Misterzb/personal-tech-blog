package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT tag_id FROM article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT article_id FROM article_tag WHERE tag_id = #{tagId}")
    List<Long> selectArticleIdsByTagId(@Param("tagId") Long tagId);

    @Delete("DELETE FROM article_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);
}
