# 廖雪峰教程 · 本地学习导入工具

## 用途与边界

- 用于个人**非商业**学习整理；导入默认为草稿（`status=0`）。
- 源站内容以 [CC BY-NC-SA 4.0](https://liaoxuefeng.com/pages/license/index.html) 发布，版权归廖雪峰。
- 导入文章文末含署名、原文链接与许可证说明。**公开发布前请自行确认合规。**
- 原始 HTML / Markdown 在 `data/`，已加入仓库 `.gitignore`。

## 范围

| 纳入 | 排除 |
|------|------|
| Java / Python / JavaScript / SQL / 手写Spring / 手写Tomcat | 区块链、Git、Makefile、博客栏目 |

## 目录划分

本站专题为单层：

- **一书 = 一个专题**（如 `lxf-java`）
- **每个有独立 URL 的章节页 = 一篇文章**（标题保留 TOC 编号，如 `14.6.1 同步方法`）
- 文内小标题留在 Markdown 正文中

## 步骤

### 1. 抓取并生成 Markdown

```bash
cd tools/liaoxuefeng-import
python crawl.py                 # 全量 6 系列
python crawl.py --series sql    # 只抓 SQL（建议先试跑）
python crawl.py --offline       # 不联网，用已有 raw 重生成 MD
```

产出：

- `data/raw/<series>/…` 原始 HTML
- `data/md/<categorySlug>/*.md`
- `data/manifest.json`

### 2. 导入博客（草稿）

先启动后端与 MySQL：

```bash
python import_drafts.py --base http://localhost:8080 --user admin --password admin123
python import_drafts.py --module lxf-sql --status 0
python import_drafts.py --prefix lxf --status 0
```

### 3. 整理标签（可选）

```bash
python sync_tags.py --base http://localhost:8080 --user admin --password admin123
```

仅系列技术标签（Java / Python / …），不加「廖雪峰」标签。

## 礼貌抓取

默认间隔 1s；请勿高频压测源站。
