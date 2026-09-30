-- 前台会员 + 评论关联
CREATE TABLE IF NOT EXISTS `member` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `phone`       VARCHAR(20)  NOT NULL                COMMENT '手机号（登录账号，唯一）',
  `password`    VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
  `nickname`    VARCHAR(64)  NOT NULL                COMMENT '显示昵称',
  `email`       VARCHAR(128) DEFAULT NULL           COMMENT '邮箱（选填）',
  `avatar`      VARCHAR(512) DEFAULT NULL           COMMENT '头像 URL',
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除：0=正常，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前台会员表';

ALTER TABLE `comment`
  ADD COLUMN `member_id` BIGINT DEFAULT NULL COMMENT '会员ID，关联 member.id' AFTER `parent_id`,
  ADD COLUMN `avatar` VARCHAR(512) DEFAULT NULL COMMENT '评论时头像快照' AFTER `email`;
