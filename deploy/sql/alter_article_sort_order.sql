-- 文章目录顺序（教程系列按 TOC 导入；0 表示未指定，列表仍可按发布时间）
ALTER TABLE `article`
  ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '专题内排序，数值越小越靠前' AFTER `is_top`;

ALTER TABLE `article`
  ADD KEY `idx_category_sort` (`category_id`, `sort_order`, `id`);
