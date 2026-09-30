# 技术实践笔记 · 个人博客

[English](README_EN.md) | 简体中文

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

Spring Boot 3 + Vue 3 前后端分离个人博客：前台阅读、会员中心、管理后台发文、专题/标签、开源项目、评论审核与回复、公告/友链、MySQL 全文检索、Redis 缓存、暗色模式与字号、RSS/SEO、访问统计。

## 仓库

| 平台 | 地址 | 主分支 |
|------|------|--------|
| GitHub | https://github.com/Misterzb/personal-tech-blog | [`main`](https://github.com/Misterzb/personal-tech-blog/tree/main) |
| Gitee | https://gitee.com/bo_live/personal-tech-blog | [`master`](https://gitee.com/bo_live/personal-tech-blog/tree/master) |

欢迎 Star / Fork / Issue / PR。贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8（FULLTEXT ngram）、Redis（可选缓存/限流） |
| 前台 | Vue 3、Vite、Vue Router、Pinia |
| 后台 | Vue 3、Element Plus、md-editor-v3 |
| 部署 | Docker Compose、Nginx（阿里云 ECS） |

## 目录结构

```
blog/
├── backend/     # API 服务
├── frontend/    # 用户前台
├── admin/       # 管理后台（构建后访问 /admin/）
├── deploy/      # docker-compose、nginx、SQL
├── tools/       # 导入/冒烟测试脚本
├── README.md    # 中文说明
└── README_EN.md # English
```

## 本地开发

### 1. 启动 MySQL（必需）

可用 Docker（宿主机端口 **3307**，避免与本机 MySQL 3306 冲突）：

```bash
cd deploy
docker compose up -d mysql
```

或使用本机 / 远程 MySQL，执行 [`deploy/sql/schema.sql`](deploy/sql/schema.sql)。**已有库**首次启动时 Flyway 会 `baseline` 当前库并执行后续迁移（如 `V2__free_soft_deleted_slugs.sql`）；也可手工执行 [`deploy/sql/alter_feature_pack.sql`](deploy/sql/alter_feature_pack.sql)。

### 2. 配置本地 / 生产（两套，互不干扰）

| 环境 | 后端 Profile | MySQL | Redis | CORS / 密钥 |
|------|--------------|-------|-------|-------------|
| **本地** | `local` → `application-local.yml` | 本机/开发实例（示例 `127.0.0.1:3307`） | **database=10** | 仅 localhost，`enforce-origin=false`；见 `.env.development` |
| **生产** | `prod` → `application-prod.yml` + `deploy/.env` | 外置 `39.107.247.76:3306/blog` | 同机 Redis **database=10** | `bo.bj.cn`，`enforce-origin=true` |

本机只起数据库：

```bash
cp deploy/.env.local.example deploy/.env.local
cd deploy && docker compose --env-file .env.local up -d mysql redis
```

本地：

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
# 编辑其中的 MySQL / Redis；JWT/AES 已与 frontend|admin/.env.development 对齐
```

生产（Docker）：

```bash
cp deploy/.env.example deploy/.env
# 编辑 SITE_ORIGIN / SITE_ORIGIN_WWW / JWT_SECRET / BLOG_RESPONSE_AES_KEY / 数据库密码
cp frontend/.env.production.example frontend/.env.production
cp admin/.env.production.example admin/.env.production
# 两处 VITE_API_AES_KEY 必须等于 deploy/.env 里的 BLOG_RESPONSE_AES_KEY
```

`application-local.yml`、`deploy/.env`、`.env.production` **不要提交 Git**。

Windows PowerShell 启动后端：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

首次启动会自动创建管理员与示例数据：

- 账号：`admin`
- 密码：`admin123`（**上线后请立刻修改**；新库管理员可能带 `must_change_password`，后台会强制改密）

### 4. 启动前台

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

会员相关：注册/登录 → `/me` 会员中心；顶栏可切换深色模式与字号。

### 5. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

访问：http://localhost:5174/admin/

新增菜单：会员、公告、友链；仪表盘含热门文章与会员增长。

### 6. 自测（可选）

```bash
# 后端集成测试（依赖 local 库）
cd backend
mvn test -Dtest=FeaturePackIntegrationTest

# 对已启动的 API 冒烟（默认 http://127.0.0.1:8080）
python tools/smoke_feature_pack.py
```

## 阿里云部署（推荐）

**建议配置**：ECS 2核4G + 域名 + SSL；安全组放行 80/443。

### 步骤

1. 将代码上传到服务器（或从对应平台 `git clone` 并检出主分支）。
2. 安装 Docker 与 Docker Compose。
3. 配置生产环境变量并构建前端（密钥与域名）：

```bash
cp deploy/.env.example deploy/.env
# 改 SITE_*、JWT_SECRET、BLOG_RESPONSE_AES_KEY、数据库/Redis 密码
cp frontend/.env.production.example frontend/.env.production
cp admin/.env.production.example admin/.env.production
# VITE_API_AES_KEY = BLOG_RESPONSE_AES_KEY
cd frontend && npm ci && npm run build && cd ..
cd admin && npm ci && npm run build && cd ..
```

4. 启动（自动加载 `deploy/.env`，`SPRING_PROFILES_ACTIVE=prod`）：

```bash
cd deploy
docker compose up -d --build
```

5. 域名解析到 ECS 公网 IP；证书可用阿里云免费 SSL 或 certbot，再按需打开 nginx HTTPS 配置。

### 访问地址

| 入口 | 路径 |
|------|------|
| 前台 | `http://你的域名/` |
| 后台 | `http://你的域名/admin/` |
| API | `http://你的域名/api/` |
| RSS | `http://你的域名/rss.xml` |
| Sitemap | `http://你的域名/sitemap.xml` |

## 功能清单

### 内容与 SEO

- 文章 Markdown 发布 / 草稿、专题、标签、置顶、专题内 `sort_order` 排序
- 开源项目卡片（GitHub / Gitee 双仓库、演示、技术栈）
- MySQL FULLTEXT（ngram）全文检索，不可用或无命中时回退 LIKE
- 搜索、RSS、sitemap、robots
- 访问 PV 统计与仪表盘（热门文章、专题阅读量、会员增长）

### 会员与互动

- 手机号注册/登录（评论须登录）
- 会员中心：资料、改密、头像、我的评论、收藏、专题订阅、通知
- 评论回复（嵌套）、审核通过后通知被回复者
- 专题阅读进度、文章收藏、系列上下篇与进度条、专题 TOC
- 站点公告、友情链接

### 管理与安全

- 后台会员列表 / 启用禁用
- 公告、友链 CRUD
- 登录 / 注册 / 评论 IP 限流
- 头像上传限制（jpg/png/gif/webp，最大 2MB）
- 管理员强制改密（`must_change_password`）

### 体验与性能

- Redis 缓存公共读接口（home / categories / tags / site / announcements / friendlinks；TTL 约 10 分钟；故障降级）
- 前台深色模式、字号（localStorage：`blog-theme` / `blog-font-size`）
- 图片上传（本地 `uploads/`，可后续切换 OSS）

### 接口防护（降低滥用）

- 公开 API IP 限流、搜索更严格限流；Nginx 层另有 `limit_req`
- 前台列表不返回正文 HTML/Markdown；详情不返回 Markdown 原文；评论隐藏邮箱/IP
- **接口 `data` 字段 AES-GCM 加密**（Network 里看到密文；前台/后台自动解密）。密钥：`blog.security.response-aes-key` 与前端 `VITE_API_AES_KEY` 一致
- 注册 / 登录 / 评论需图形验证码（`GET /api/public/captcha`）；脚本自测可设 `BLOG_CAPTCHA_ENABLED=false`
- 探活：`GET /actuator/health`；未捕获异常可经 `blog.alert.*` Webhook 告警（企微/钉钉/飞书）
- 安全响应头；生产建议 HTTPS + `blog.security.enforce-origin=true`（见 [SECURITY.md](SECURITY.md)）
- 说明：密钥仍会打进前端包，可防随手查看与简单爬虫，不能替代 HTTPS 与权限控制；公开文章内容本身仍可被阅读

## 关键与补丁

| 文件 | 用途 |
|------|------|
| [`deploy/sql/schema.sql`](deploy/sql/schema.sql) | 全新库手工初始化参考（与 Flyway `V1` 对齐） |
| [`backend/.../db/migration/`](backend/src/main/resources/db/migration) | **Flyway** 正式迁移（启动自动执行） |
| [`deploy/sql/alter_feature_pack.sql`](deploy/sql/alter_feature_pack.sql) | 旧库手工增量参考 |

## 后续可选

- 阿里云 OSS 存图片
- 微信公众号文章同步
- 邮件通知 / 订阅推送

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

安全问题请参阅 [SECURITY.md](SECURITY.md)；变更记录见 [CHANGELOG.md](CHANGELOG.md)。
