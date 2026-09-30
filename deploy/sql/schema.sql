-- =============================================================================
-- 个人技术博客 · 数据库初始化脚本
-- 数据库：blog
-- 字符集：utf8mb4 / utf8mb4_unicode_ci（兼容 emoji 与多语言）
-- 说明：表结构含逻辑删除字段 deleted（0=正常，1=已删除），业务查询需过滤
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `blog`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT '个人技术博客业务库';

USE `blog`;

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 表：user（后台管理员账号）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    VARCHAR(64)  NOT NULL                COMMENT '登录用户名（唯一）',
  `password`    VARCHAR(255) NOT NULL                COMMENT '登录密码（BCrypt 加密存储）',
  `nickname`    VARCHAR(64)  DEFAULT NULL           COMMENT '显示昵称',
  `avatar`      VARCHAR(512) DEFAULT NULL           COMMENT '头像 URL',
  `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '1=登录后必须修改密码',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员账号表';

-- -----------------------------------------------------------------------------
-- 表：member（前台会员）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone`       VARCHAR(20)  NOT NULL                COMMENT '手机号（登录账号，唯一）',
  `password`    VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
  `nickname`    VARCHAR(64)  NOT NULL                COMMENT '显示昵称',
  `email`       VARCHAR(128) DEFAULT NULL           COMMENT '邮箱（选填）',
  `avatar`      VARCHAR(512) DEFAULT NULL           COMMENT '头像 URL',
  `status`      TINYINT      NOT NULL DEFAULT 1    COMMENT '账号状态：0=禁用，1=正常',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前台会员表';

-- -----------------------------------------------------------------------------
-- 表：category（文章专题/分类）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '专题名称',
  `slug`        VARCHAR(128) NOT NULL                COMMENT 'URL 友好标识（唯一，用于前台路由）',
  `description` VARCHAR(512) DEFAULT NULL           COMMENT '专题简介',
  `cover`       VARCHAR(512) DEFAULT NULL           COMMENT '专题封面图 URL',
  `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重，数值越小越靠前',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章专题（分类）表';

-- -----------------------------------------------------------------------------
-- 表：tag（文章标签）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(64)  NOT NULL                COMMENT '标签名称',
  `slug`        VARCHAR(128) NOT NULL                COMMENT 'URL 友好标识（唯一）',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签表';

-- -----------------------------------------------------------------------------
-- 表：article（文章主表）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `article` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`           VARCHAR(200) NOT NULL                COMMENT '文章标题',
  `slug`            VARCHAR(220) NOT NULL                COMMENT 'URL 友好标识（唯一，用于详情页路由）',
  `summary`         VARCHAR(500) DEFAULT NULL           COMMENT '文章摘要（列表页展示）',
  `content_md`      MEDIUMTEXT                          COMMENT '正文 Markdown 原文',
  `content_html`    MEDIUMTEXT                          COMMENT '正文 HTML 缓存（由 Markdown 渲染生成）',
  `cover`           VARCHAR(512) DEFAULT NULL           COMMENT '封面图 URL',
  `status`          TINYINT      NOT NULL DEFAULT 0    COMMENT '发布状态：0=草稿，1=已发布',
  `category_id`     BIGINT       DEFAULT NULL           COMMENT '所属专题ID，关联 category.id',
  `view_count`      BIGINT       NOT NULL DEFAULT 0    COMMENT '阅读量（PV）',
  `seo_title`       VARCHAR(200) DEFAULT NULL           COMMENT 'SEO 标题（为空时可用文章标题）',
  `seo_description` VARCHAR(500) DEFAULT NULL           COMMENT 'SEO 描述（为空时可用摘要）',
  `is_top`          TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '是否置顶：0=否，1=是',
  `sort_order`      INT          NOT NULL DEFAULT 0    COMMENT '专题内排序，数值越小越靠前（教程 TOC 等）',
  `published_at`    DATETIME     DEFAULT NULL           COMMENT '首次发布时间（发布时写入）',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slug` (`slug`),
  KEY `idx_category` (`category_id`),
  KEY `idx_category_sort` (`category_id`, `sort_order`, `id`),
  KEY `idx_status_published` (`status`, `published_at`),
  FULLTEXT KEY `ft_article_search` (`title`, `summary`, `content_md`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章主表';

-- -----------------------------------------------------------------------------
-- 表：article_tag（文章-标签关联）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `article_tag` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL                COMMENT '文章ID，关联 article.id',
  `tag_id`     BIGINT NOT NULL                COMMENT '标签ID，关联 tag.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章与标签多对多关联表';

-- -----------------------------------------------------------------------------
-- 表：project（开源项目展示）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `project` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(128) NOT NULL                COMMENT '项目名称',
  `summary`     VARCHAR(500) DEFAULT NULL           COMMENT '项目简介',
  `tech_stack`  VARCHAR(255) DEFAULT NULL           COMMENT '技术栈（逗号分隔，如 Java,Spring Boot,Vue）',
  `repo_url`    VARCHAR(512) DEFAULT NULL           COMMENT '兼容旧字段：主仓库地址（可作 GitHub 回退）',
  `github_url`  VARCHAR(512) DEFAULT NULL           COMMENT 'GitHub 仓库地址',
  `gitee_url`   VARCHAR(512) DEFAULT NULL           COMMENT 'Gitee 仓库地址',
  `demo_url`    VARCHAR(512) DEFAULT NULL           COMMENT '在线演示地址',
  `cover`       VARCHAR(512) DEFAULT NULL           COMMENT '项目封面图 URL',
  `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重，数值越小越靠前',
  `is_top`      TINYINT(1)   NOT NULL DEFAULT 0    COMMENT '是否置顶：0=否，1=是',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开源项目展示表';

-- -----------------------------------------------------------------------------
-- 表：comment（文章评论）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT        NOT NULL                COMMENT '所属文章ID，关联 article.id',
  `parent_id`  BIGINT        NOT NULL DEFAULT 0    COMMENT '父评论ID：0=顶级评论，非0=回复某条评论',
  `member_id`  BIGINT        DEFAULT NULL           COMMENT '会员ID，关联 member.id',
  `reply_to_member_id` BIGINT DEFAULT NULL          COMMENT '被回复的会员ID',
  `nickname`   VARCHAR(64)   NOT NULL                COMMENT '评论者昵称（快照）',
  `email`      VARCHAR(128)  DEFAULT NULL           COMMENT '评论者邮箱（快照，可选）',
  `avatar`     VARCHAR(512)  DEFAULT NULL           COMMENT '评论时头像快照',
  `content`    VARCHAR(1000) NOT NULL                COMMENT '评论正文',
  `status`     TINYINT       NOT NULL DEFAULT 0    COMMENT '审核状态：0=待审核，1=已通过，2=已拒绝',
  `ip`         VARCHAR(64)   DEFAULT NULL           COMMENT '评论者 IP（脱敏后存储）',
  `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted`    TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除标记：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_article_status` (`article_id`, `status`),
  KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论表（需后台审核后前台展示）';

-- -----------------------------------------------------------------------------
-- 表：member_notification（会员通知）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member_notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`   BIGINT       NOT NULL                COMMENT '接收会员',
  `type`        VARCHAR(32)  NOT NULL DEFAULT 'comment_reply' COMMENT '类型',
  `title`       VARCHAR(128) NOT NULL                COMMENT '标题',
  `content`     VARCHAR(512) DEFAULT NULL           COMMENT '内容',
  `related_id`  BIGINT       DEFAULT NULL           COMMENT '关联ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0    COMMENT '0=未读 1=已读',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_read` (`member_id`, `is_read`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员通知';

-- -----------------------------------------------------------------------------
-- 表：member_reading_progress（专题阅读进度）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member_reading_progress` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT   NOT NULL,
  `category_id` BIGINT   NOT NULL,
  `article_id`  BIGINT   NOT NULL,
  `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_category` (`member_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专题阅读进度';

-- -----------------------------------------------------------------------------
-- 表：member_favorite（文章收藏）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member_favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT   NOT NULL,
  `article_id`  BIGINT   NOT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_article` (`member_id`, `article_id`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏';

-- -----------------------------------------------------------------------------
-- 表：member_category_sub（专题订阅）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `member_category_sub` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT   NOT NULL,
  `category_id` BIGINT   NOT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_category` (`member_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专题订阅';

-- -----------------------------------------------------------------------------
-- 表：announcement（站点公告）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `announcement` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(200) NOT NULL,
  `content`     VARCHAR(2000) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=下线 1=上线',
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点公告';

-- -----------------------------------------------------------------------------
-- 表：friend_link（友情链接）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `friend_link` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64)  NOT NULL,
  `url`         VARCHAR(512) NOT NULL,
  `logo`        VARCHAR(512) DEFAULT NULL,
  `description` VARCHAR(256) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=下线 1=上线',
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='友情链接';

-- -----------------------------------------------------------------------------
-- 表：site_config（站点全局配置，通常仅一条记录）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `site_config` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `site_name`     VARCHAR(128) NOT NULL                COMMENT '站点名称',
  `site_subtitle` VARCHAR(255) DEFAULT NULL           COMMENT '站点副标题/一句话定位',
  `icp`           VARCHAR(128) DEFAULT NULL           COMMENT 'ICP 备案号',
  `about_md`      MEDIUMTEXT                          COMMENT '「关于我」页 Markdown 原文',
  `about_html`    MEDIUMTEXT                          COMMENT '「关于我」页 HTML 缓存',
  `social_links`  TEXT                                COMMENT '社交链接 JSON，如 [{"name":"GitHub","url":"..."}]',
  `logo`          VARCHAR(512) DEFAULT NULL           COMMENT '站点 Logo URL',
  `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点全局配置表（单行配置）';

-- -----------------------------------------------------------------------------
-- 表：visit_log（访问明细日志）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `visit_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `path`       VARCHAR(512) NOT NULL                COMMENT '访问路径（如 /articles/welcome）',
  `ip_mask`    VARCHAR(64)  DEFAULT NULL           COMMENT '脱敏后的客户端 IP',
  `user_agent` VARCHAR(255) DEFAULT NULL           COMMENT 'User-Agent（截断存储）',
  `visit_date` DATE         NOT NULL                COMMENT '访问日期（用于按日聚合）',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面访问明细日志表';

-- -----------------------------------------------------------------------------
-- 表：daily_stat（按日 PV 聚合统计）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `daily_stat` (
  `id`        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` DATE   NOT NULL                COMMENT '统计日期',
  `pv`        BIGINT NOT NULL DEFAULT 0    COMMENT '当日页面浏览量（PV）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按日访问量（PV）聚合统计表';
