# 吴师兄大模型 · 本地学习导入工具

## 用途与边界

- 用于个人学习整理；导入时可选择草稿（`status=0`）或发布（`status=1`）。
- 第三方内容版权归原作者 [吴师兄大模型](https://www.wushixiongai.com/)。**公开发布前请自行确认版权合规。**
- 原始 JSON/HTML 在 `data/`，已加入仓库 `.gitignore`。
- 每篇文章文末有来源声明；项目面试题含「同系列导航」链接到本站上下题。

## 数据结构

- **一个知识模块 = 一个专题**（如 RAG基础 → `study-rag`）
- **一道题 = 一篇文章**，挂在对应专题下

## 步骤

### 1. 抓取并生成 Markdown

```bash
cd tools/wushixiong-import
python crawl.py
```

仅重解析已保存的项目面试 HTML（不联网，修复排版时用）：

```bash
python crawl.py --offline-project
```

仅重生成题库 Markdown（来源改文末，不联网）：

```bash
python crawl.py --offline-bank
```

产出：

- `data/raw/` 原始 JSON / HTML
- `data/md/<categorySlug>/*.md` 一题一文
- `data/manifest.json` 导入清单

### 2. 导入博客

先启动后端与 MySQL，再执行：

```bash
# 默认导入为草稿
python import_drafts.py --base http://localhost:8080 --user admin --password admin123

# 只导入某专题
python import_drafts.py --module study-rag

# 项目面试题重导并发布到前台
python import_drafts.py --prefix study-proj --status 1
```

### 3. 整理标签（推荐在导入后执行）

按专题绑定稳定标签（如 Prompt / Agent / RAG），重挂文章关联并删除空壳标签：

```bash
python sync_tags.py --base http://localhost:8080 --user admin --password admin123
```

规则简述：

| 专题前缀 | 标签 |
|----------|------|
| `study-prompt` 等题库专题 | 主题标签（Prompt/Agent/…）+ `面试题` |
| `study-proj-*` | `项目面试` |

前台 `/api/public/tags` 只返回「至少有一篇已发布文章」的标签，避免点到空标签。

### 4. 阅读

- 草稿：管理后台 → 文章管理
- 已发布：前台专题 / 文章详情；同系列「上一题 / 下一题」可点击跳转

## 数据来源

| 类型 | 入口 |
|------|------|
| 题库 | `https://www.wushixiongai.com/questions/llm/*.json` |
| 项目面试题 | `https://www.wushixiongai.com/questions/project` 及子页 |

项目页解析只保留正文（`project-quick-answer` / `project-article-body` / 追问 / 项目自检），剥离侧栏题库、页内 TOC、同系列卡片与 CTA；上下题改为本站 `/articles/...` 链接。

抓取间隔默认 1s，请保持礼貌频率。
