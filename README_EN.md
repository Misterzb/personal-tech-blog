# Technical Practice Notes · Personal Blog

[English](README_EN.md) | [简体中文](README.md)

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)

A personal blog built with Spring Boot 3 and Vue 3: public reading site, admin publishing console, categories/tags, open-source project showcase, moderated comments, RSS/SEO, and visit stats.

## Repositories

| Platform | URL | Default branch |
|----------|-----|----------------|
| GitHub | https://github.com/Misterzb/personal-tech-blog | [`main`](https://github.com/Misterzb/personal-tech-blog/tree/main) |
| Gitee | https://gitee.com/bo_live/personal-tech-blog | [`master`](https://gitee.com/bo_live/personal-tech-blog/tree/master) |

Stars, forks, issues, and PRs are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before contributing.

## Stack

| Module | Tech |
|--------|------|
| Backend | Spring Boot 3, Spring Security, JWT, MyBatis-Plus, MySQL 8 |
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
├── README.md    # Chinese
└── README_EN.md # English
```

## Local development

### 1. MySQL

Docker (host port **3307**):

```bash
cd deploy
docker compose up -d mysql
```

Or use a local MySQL instance with [`deploy/sql/schema.sql`](deploy/sql/schema.sql):

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
# then edit DB credentials
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

First boot creates an admin user and sample data:

- Username: `admin`
- Password: `admin123` (**change immediately in production**)

### 3. Public frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

### 4. Admin frontend

```bash
cd admin
npm install
npm run dev
```

Open http://localhost:5174/admin/

## Production deploy (example)

Suggested: 2C4G VM + domain + TLS; open ports 80/443.

1. Upload or `git clone` and check out the platform default branch (`main` / `master`).
2. Install Docker and Docker Compose.
3. Build static assets:

```bash
cd frontend && npm ci && npm run build && cd ..
cd admin && npm ci && npm run build && cd ..
```

4. Edit [`deploy/docker-compose.yml`](deploy/docker-compose.yml): `SITE_BASE_URL`, DB password, `JWT_SECRET`.
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

- Markdown articles (draft/publish), categories, tags, pin-to-top
- Open-source project cards (GitHub + Gitee, demo, tech stack)
- Comments with admin moderation
- Search, RSS, sitemap, robots
- Visit PV stats and dashboard
- Local image upload (`uploads/`; OSS optional later)

## License

Released under the [MIT License](LICENSE).

See [SECURITY.md](SECURITY.md) for security reporting and [CHANGELOG.md](CHANGELOG.md) for release notes.
