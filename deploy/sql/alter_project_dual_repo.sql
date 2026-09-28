-- 开源项目支持 GitHub + Gitee 双仓库地址（已有库执行一次即可）
-- 若列已存在会报错，可忽略后继续执行下方 UPDATE

ALTER TABLE `project`
  ADD COLUMN `github_url` VARCHAR(512) DEFAULT NULL COMMENT 'GitHub 仓库地址' AFTER `repo_url`;

ALTER TABLE `project`
  ADD COLUMN `gitee_url` VARCHAR(512) DEFAULT NULL COMMENT 'Gitee 仓库地址' AFTER `github_url`;

UPDATE `project`
SET `github_url` = `repo_url`
WHERE (`github_url` IS NULL OR `github_url` = '')
  AND `repo_url` LIKE '%github.com%';

UPDATE `project`
SET `gitee_url` = `repo_url`
WHERE (`gitee_url` IS NULL OR `gitee_url` = '')
  AND `repo_url` LIKE '%gitee.com%';

UPDATE `project`
SET `github_url` = `repo_url`
WHERE (`github_url` IS NULL OR `github_url` = '')
  AND (`gitee_url` IS NULL OR `gitee_url` = '')
  AND `repo_url` IS NOT NULL
  AND `repo_url` <> '';
