# 技术实践笔记 · 个人博客

Spring Boot 3 + Vue 3 前后端分离个人博客：前台阅读、管理后台发文、专题/标签、开源项目、评论审核、RSS/SEO、访问统计。

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
└── README.md
```

## 本地开发

### 1. 启动 MySQL

可用 Docker（宿主机端口 **3307**，避免与本机 MySQL 3306 冲突）：

```bash
cd deploy
docker compose up -d mysql
```

或使用本机 MySQL，执行 [`deploy/sql/schema.sql`](deploy/sql/schema.sql)，并保证账号密码与 `backend/src/main/resources/application.yml` 一致（默认 `root` / `root123`，库名 `blog`）。

### 2. 启动后端

```bash
cd backend
# 连接 Docker MySQL(3307)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或连接本机 3306（默认 application.yml）
mvn spring-boot:run
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

1. 将代码上传到服务器（或 `git clone`）。
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

个人项目，按需自用或开源。
