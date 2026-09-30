-- 释放软删除文章占用的 slug，避免唯一键冲突
UPDATE article
SET slug = LEFT(CONCAT(slug, '-deleted-', id), 220)
WHERE deleted = 1
  AND slug NOT LIKE '%-deleted-%'
  AND slug NOT LIKE 'lxf-trash-%';
