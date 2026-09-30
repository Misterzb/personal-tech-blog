# Technical Practice Notes · Personal Blog

[English](README_EN.md) | [简体中文](README.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

A personal blog built with Spring Boot 3 and Vue 3: public reading site, member center, admin publishing console, categories/tags, open-source showcase, moderated comments with replies, announcements/friend links, MySQL full-text search, optional Redis cache, dark mode & font size, RSS/SEO, and visit stats.

## Repositories

| Platform | URL | Default branch |
|----------|-----|----------------|
| GitHub | https://github.com/Misterzb/personal-tech-blog | [`main`](https://github.com/Misterzb/personal-tech-blog/tree/main) |
| Gitee | https://gitee.com/bo_live/personal-tech-blog | [`master`](https://gitee.com/bo_live/personal-tech-blog/tree/master) |

Stars, forks, issues, and PRs are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before contributing.

## Stack

| Module | Tech |
|--------|------|
| Backend | Spring Boot 3, Spring Security, JWT, MyBatis-Plus, MySQL 8 (FULLTEXT ngram), Redis (optional cache/rate-limit) |
| Public site | Vue 3, Vite, Vue Router, Pinia |
| Admin | Vue 3, Element Plus, md-editor-v3 |
| Deploy | Docker Compose, Nginx |

## Layout

```
blog/
├── backend/     # API
├── frontend/    # Public site
├── admin/       # Admin console (served under /admin/ after build)
├── deploy/      # docker-compose, nginx, SQL
├── tools/       # import / smoke scripts
├── README.md    # Chinese
└── README_EN.md # English
```

## Local development

### 1. MySQL (required)

Docker (host port **3307**):

```bash
cd deploy
docker compose up -d mysql
```

Or use a local/remote MySQL with [`deploy/sql/schema.sql`](deploy/sql/schema.sql). For existing databases, Flyway baselines on first boot and applies later migrations (e.g. `V2__free_soft_deleted_slugs.sql`); you may also run [`deploy/sql/alter_feature_pack.sql`](deploy/sql/alter_feature_pack.sql) manually.

### 2. Local config

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
```

Edit **MySQL** and optional **Redis** settings. Do **not** commit `application-local.yml`.

| Key | Notes |
|-----|--------|
| `spring.datasource.*` | Database |
| `spring.data.redis.*` | Cache & rate limiting; app still starts if Redis is down |
| Redis `database` | Prefer a dedicated DB index (example uses `10`) |

On Windows PowerShell, quote Maven args:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 3. Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

First boot creates an admin user and sample data:

- Username: `admin`
- Password: `admin123` (**change immediately in production**; new installs may force password change via `must_change_password`)

### 4. Public frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

Members: register/login → `/me`; header toggles dark mode and font size.

### 5. Admin frontend

```bash
cd admin
npm install
npm run dev
```

Open http://localhost:5174/admin/

Extra menus: Members, Announcements, Friend links; dashboard includes hot articles and member growth.

### 6. Tests (optional)

```bash
cd backend
mvn test -Dtest=FeaturePackIntegrationTest

python tools/smoke_feature_pack.py
```

## Production deploy (example)

Suggested: 2C4G VM + domain + TLS; open ports 80/443.

1. Upload or `git clone` and check out the platform default branch (`main` / `master`).
2. Install Docker and Docker Compose.
3. Build static assets:

```bash
cd frontend && npm ci && npm run build && cd ..
cd admin && npm ci && npm run build && cd ..
```

4. Edit [`deploy/docker-compose.yml`](deploy/docker-compose.yml): `SITE_BASE_URL`, DB password, `JWT_SECRET`, Redis password, etc.
5. Start:

```bash
cd deploy
docker compose up -d --build
```

### Endpoints

| Entry | Path |
|-------|------|
| Site | `http://your-domain/` |
| Admin | `http://your-domain/admin/` |
| API | `http://your-domain/api/` |
| RSS | `http://your-domain/rss.xml` |
| Sitemap | `http://your-domain/sitemap.xml` |

## Features

### Content & SEO

- Markdown articles (draft/publish), categories, tags, pin-to-top, in-category `sort_order`
- Open-source project cards (GitHub + Gitee, demo, tech stack)
- MySQL FULLTEXT (ngram) search with LIKE fallback
- Search, RSS, sitemap, robots
- Visit PV stats and dashboard (hot articles, category views, member growth)

### Members & engagement

- Phone register/login (comments require login)
- Member center: profile, password, avatar, my comments, favorites, category subscriptions, notifications
- Nested comment replies; notify parent author when a reply is approved
- Reading progress, favorites, series prev/next + progress, category TOC
- Announcements and friend links

### Admin & security

- Member list / enable-disable
- Announcement & friend-link CRUD
- Rate limits on login/register/comment
- Avatar upload limits (jpg/png/gif/webp, max 2MB)
- Forced admin password change (`must_change_password`)

### UX & performance

- Redis cache for public reads (TTL ~10 min; graceful degradation)
- Dark mode & font size (`blog-theme` / `blog-font-size` in localStorage)
- Local image upload (`uploads/`; OSS optional later)

### API protection

- IP rate limits on public APIs (stricter for search); Nginx `limit_req` as well
- Public lists omit body HTML/Markdown; article detail omits Markdown source; comments hide email/IP
- Captcha on register / login / comment (`GET /api/public/captcha`); set `BLOG_CAPTCHA_ENABLED=false` for scripts
- Health: `GET /actuator/health`; optional error Webhook via `blog.alert.*`
- Security headers; production should use HTTPS + `blog.security.enforce-origin=true` (see [SECURITY.md](SECURITY.md))
- Note: published articles are meant to be readable; protection focuses on abuse control and redaction, not “unreadable encrypted public content”

## Schema

| File | Purpose |
|------|---------|
| [`deploy/sql/schema.sql`](deploy/sql/schema.sql) | Fresh install reference (aligned with Flyway `V1`) |
| [`backend/.../db/migration/`](backend/src/main/resources/db/migration) | **Flyway** migrations (applied on startup) |
| [`deploy/sql/alter_feature_pack.sql`](deploy/sql/alter_feature_pack.sql) | Manual upgrade reference |

## Roadmap (optional)

- Alibaba Cloud OSS for images
- WeChat Official Account sync
- Email / subscription push

## License

Released under the [MIT License](LICENSE).

See [SECURITY.md](SECURITY.md) for security reporting and [CHANGELOG.md](CHANGELOG.md) for release notes.
