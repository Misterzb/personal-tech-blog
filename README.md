# 技术实践笔记 · 个人博客

[English](README_EN.md) | 简体中文

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

Spring Boot 3 + Vue 3 前后端分离个人博客：前台阅读、管理后台发文、专题/标签、开源项目、评论审核、RSS/SEO、访问统计。

## 仓库

| 平台 | 地址 | 主分支 |
|------|------|--------|
| GitHub | https://github.com/Misterzb/personal-tech-blog | [`main`](https://github.com/Misterzb/personal-tech-blog/tree/main) |
| Gitee | https://gitee.com/bo_live/personal-tech-blog | [`master`](https://gitee.com/bo_live/personal-tech-blog/tree/master) |

欢迎 Star / Fork / Issue / PR。贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL 8 |
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
├── README.md    # 中文说明
└── README_EN.md # English
```

## 本地开发

### 1. 启动 MySQL

可用 Docker（宿主机端口 **3307**，避免与本机 MySQL 3306 冲突）：

```bash
cd deploy
docker compose up -d mysql
```

或使用本机 MySQL，执行 [`deploy/sql/schema.sql`](deploy/sql/schema.sql)，并复制配置：

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
# 再编辑其中的数据库连接信息
```

### 2. 启动后端

```bash
cd backend
# 使用 application-local.yml（请先复制 example）
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

首次启动会自动创建管理员与示例数据：

- 账号：`admin`
- 密码：`admin123`（**上线后请立刻修改**）

### 3. 启动前台

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

### 4. 启动管理后台

```bash
cd admin
npm install
npm run dev
```

访问：http://localhost:5174/admin/

## 阿里云部署（推荐）

**建议配置**：ECS 2核4G + 域名 + SSL；安全组放行 80/443。

### 步骤

1. 将代码上传到服务器（或从对应平台 `git clone` 并检出主分支）。
2. 安装 Docker 与 Docker Compose。
3. 构建前端静态资源：

```bash
cd frontend && npm ci && npm run build && cd ..
cd admin && npm ci && npm run build && cd ..
```

4. 修改 [`deploy/docker-compose.yml`](deploy/docker-compose.yml) 中 `SITE_BASE_URL`、数据库密码、`JWT_SECRET`。
5. 启动：

```bash
cd deploy
docker compose up -d --build
```

6. 域名解析到 ECS 公网 IP；证书可用阿里云免费 SSL 或 certbot，再按需打开 nginx HTTPS 配置。

### 访问地址

| 入口 | 路径 |
|------|------|
| 前台 | `http://你的域名/` |
| 后台 | `http://你的域名/admin/` |
| API | `http://你的域名/api/` |
| RSS | `http://你的域名/rss.xml` |
| Sitemap | `http://你的域名/sitemap.xml` |

## 功能清单

- 文章 Markdown 发布 / 草稿、专题、标签、置顶
- 开源项目卡片（GitHub / Gitee 双仓库、演示、技术栈）
- 评论提交 + 后台审核
- 搜索、RSS、sitemap、robots
- 访问 PV 统计与仪表盘
- 图片上传（本地 `uploads/`，可后续切换 OSS）

## 后续可选

- Redis 缓存热门文章与统计
- 阿里云 OSS 存图片
- 微信公众号文章同步
- 管理员改密接口与强制首次改密

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

安全问题请参阅 [SECURITY.md](SECURITY.md)；变更记录见 [CHANGELOG.md](CHANGELOG.md)。
