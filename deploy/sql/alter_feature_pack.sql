-- =============================================================================
-- Feature pack 增量补丁（已有库执行；列/表已存在可忽略报错）
-- 与 SchemaPatchRunner 保持一致
-- =============================================================================

USE `blog`;

-- 管理员强制改密
ALTER TABLE `user`
  ADD COLUMN `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '1=须改密' AFTER `avatar`;

-- 会员状态
ALTER TABLE `member`
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常' AFTER `avatar`;

-- 评论回复目标
ALTER TABLE `comment`
  ADD COLUMN `reply_to_member_id` BIGINT DEFAULT NULL COMMENT '回复目标会员ID' AFTER `member_id`;

-- 会员通知
CREATE TABLE IF NOT EXISTS `member_notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT       NOT NULL,
  `type`        VARCHAR(32)  NOT NULL DEFAULT 'system',
  `title`       VARCHAR(128) NOT NULL,
  `content`     VARCHAR(512) DEFAULT NULL,
  `related_id`  BIGINT       DEFAULT NULL,
  `is_read`     TINYINT      NOT NULL DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member_read` (`member_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员通知';

-- 阅读进度
CREATE TABLE IF NOT EXISTS `member_reading_progress` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT   NOT NULL,
  `category_id` BIGINT   NOT NULL,
  `article_id`  BIGINT   NOT NULL,
  `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_category` (`member_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员专题阅读进度';

-- 收藏
CREATE TABLE IF NOT EXISTS `member_favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`  BIGINT   NOT NULL,
  `article_id` BIGINT   NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_article` (`member_id`, `article_id`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员收藏';

-- 专题订阅
CREATE TABLE IF NOT EXISTS `member_category_sub` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `member_id`   BIGINT   NOT NULL,
  `category_id` BIGINT   NOT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_category` (`member_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员专题订阅';

-- 公告
CREATE TABLE IF NOT EXISTS `announcement` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `title`      VARCHAR(200) NOT NULL,
  `content`    TEXT,
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
  `sort_order` INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点公告';

-- 友链
CREATE TABLE IF NOT EXISTS `friend_link` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(128) NOT NULL,
  `url`         VARCHAR(512) NOT NULL,
  `logo`        VARCHAR(512) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0=停用 1=启用',
  `sort_order`  INT          NOT NULL DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='友情链接';

-- 文章全文检索（ngram）
ALTER TABLE `article`
  ADD FULLTEXT INDEX `ft_article_search` (`title`, `summary`, `content_md`) WITH PARSER ngram;
