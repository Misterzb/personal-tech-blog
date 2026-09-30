# Changelog

本项目遵循简明变更记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### Added

- **会员体系**：手机号注册/登录、会员中心（资料/密码/头像/我的评论/收藏/订阅/通知）
- **评论增强**：嵌套回复、审核通过后通知被回复者；后台按会员筛选
- **阅读体验**：专题阅读进度、文章收藏、系列上下篇与进度、专题 TOC
- **检索**：MySQL FULLTEXT（ngram），失败或无命中时回退 LIKE
- **运营**：站点公告、友情链接；仪表盘热门文章 / 专题阅读量 / 会员增长
- **安全**：登录注册评论限流、头像类型与大小限制、管理员 `must_change_password` 强制改密、会员启停
- **验证码**：注册 / 登录 / 评论图形验证码（`/api/public/captcha`）
- **探活与告警**：Actuator `/actuator/health`；可选 Webhook 错误告警（企微/钉钉/飞书）
- **缓存**：可选 Redis（公共读接口 TTL 约 10 分钟，故障降级；Jackson + JavaTime 序列化）
- **前台偏好**：深色模式、字号（localStorage）
- **Flyway**：`db/migration` 接管 schema；移除 `SchemaPatchRunner`
- SQL：`deploy/sql/alter_feature_pack.sql`；`schema.sql` 同步功能包字段与表
- 测试：`FeaturePackIntegrationTest`、`tools/smoke_feature_pack.py`
- 开源配套文件：`LICENSE`（MIT）、`CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`
- 开源项目支持 GitHub / Gitee 双仓库地址
- 中英双份 README（`README.md` / `README_EN.md`）

### Changed

- 文章详情页采用左列表 / 中正文 / 右目录三栏阅读布局
- 文档默认分支改为各平台主分支（GitHub `main`，Gitee `master`）
- `application-local.yml.example` 增加 Redis / 告警示例；Docker Compose 增加 Redis 服务与后端环境变量
- 评论须会员登录后提交
- 数据库变更改为 Flyway 迁移，不再使用启动时 SchemaPatchRunner
- **本地 / 生产配置分离**：`local` 仅 localhost CORS；`prod` 正式域名 + `enforce-origin=true`；密钥走 `deploy/.env` 与 `.env.production`

## [0.1.0] - 2026-09-28

### Added

- Spring Boot 3 + Vue 3 个人博客初版（前台、后台、专题/标签、开源项目、评论、RSS/SEO）
- Docker Compose / Nginx 部署配置
- 本地学习导入工具 `tools/wushixiong-import`（仅个人学习整理，请勿滥用）
