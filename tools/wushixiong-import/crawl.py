"""
吴师兄大模型题库/项目面试题 — 本地学习抓取脚本
个人学习整理；发布第三方内容请自行确认版权合规。
"""
from __future__ import annotations

import argparse
import html as html_lib
import json
import re
import time
import urllib.request
from pathlib import Path

BASE = "https://www.wushixiongai.com"
UA = "PersonalLocalStudyBot/1.0 (+local learning draft import; contact: local)"
DELAY = 1.0
ROOT = Path(__file__).resolve().parent
RAW = ROOT / "data" / "raw"
MD = ROOT / "data" / "md"

LLM_FILES = [
    "rag-basic.json",
    "retrieval.json",
    "rerank.json",
    "document.json",
    "knowledge-graph.json",
    "multimodal.json",
    "agent.json",
    "prompt.json",
    "finetune.json",
    "evaluation.json",
    "2026-real.json",
    "intro.json",
]

MODULE_SLUG = {
    "RAG基础": "study-rag",
    "RAG 基础": "study-rag",
    "向量检索": "study-vector",
    "重排与优化": "study-rerank",
    "文档处理": "study-doc",
    "知识图谱": "study-kg",
    "多模态": "study-multimodal",
    "Agent": "study-agent",
    "Prompt工程": "study-prompt",
    "Prompt 工程": "study-prompt",
    "模型微调": "study-finetune",
    "评估与监控": "study-eval",
    "模型架构": "study-arch",
    "推理优化": "study-infer",
    "模型训练": "study-train",
    "RLHF与对齐": "study-rlhf",
    "RLHF 与对齐": "study-rlhf",
    "项目与经历": "study-experience",
    "项目面试·RAG": "study-proj-rag",
    "项目面试·Deep Research": "study-proj-deep-research",
    "项目面试·Agent": "study-proj-agent",
}

EYEBROW_RE = re.compile(
    r"^(INTERVIEW FOLLOW-UPS|PROJECT CHECK|KEEP PRACTICING|AGENT PROJECT INTERVIEW.*)$",
    re.I,
)


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def fetch_text(url: str) -> str:
    return fetch(url).decode("utf-8", errors="replace")


def slugify_module(name: str) -> str:
    if name in MODULE_SLUG:
        return MODULE_SLUG[name]
    s = re.sub(r"[^\w\u4e00-\u9fff]+", "-", name).strip("-").lower()
    return "study-" + (s or "misc")


def article_slug(module: str, slug: str) -> str:
    return f"{slugify_module(module)}-{slug}"[:200]


def unescape(text: str) -> str:
    return html_lib.unescape(text or "")


def strip_tags(fragment: str) -> str:
    t = re.sub(r"<br\s*/?>", "\n", fragment, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    return unescape(t).strip()


def extract_by_class(html: str, class_name: str, tag: str | None = None) -> str | None:
    """Extract first element whose class list contains class_name (depth-balanced)."""
    if tag:
        pat = rf"<{tag}\b[^>]*class=\"[^\"]*\b{re.escape(class_name)}\b[^\"]*\"[^>]*>"
    else:
        pat = rf"<([a-zA-Z0-9]+)[^>]*class=\"[^\"]*\b{re.escape(class_name)}\b[^\"]*\"[^>]*>"
    m = re.search(pat, html, re.I)
    if not m:
        return None
    open_tag = m.group(0)
    tag_name = tag or m.group(1)
    start = m.end()
    depth = 1
    i = start
    open_re = re.compile(rf"<{tag_name}\b[^>]*>", re.I)
    close_re = re.compile(rf"</{tag_name}\s*>", re.I)
    while i < len(html) and depth > 0:
        om = open_re.search(html, i)
        cm = close_re.search(html, i)
        if not cm:
            return None
        if om and om.start() < cm.start():
            # self-closing-ish skip if ends with />
            chunk = om.group(0)
            if chunk.rstrip().endswith("/>"):
                i = om.end()
                continue
            depth += 1
            i = om.end()
        else:
            depth -= 1
            if depth == 0:
                return html[start : cm.start()]
            i = cm.end()
    return None


def extract_by_id(html: str, elem_id: str) -> str | None:
    m = re.search(
        rf"<([a-zA-Z0-9]+)[^>]*\bid=\"{re.escape(elem_id)}\"[^>]*>",
        html,
        re.I,
    )
    if not m:
        return None
    tag_name = m.group(1)
    start = m.end()
    depth = 1
    i = start
    open_re = re.compile(rf"<{tag_name}\b[^>]*>", re.I)
    close_re = re.compile(rf"</{tag_name}\s*>", re.I)
    while i < len(html) and depth > 0:
        om = open_re.search(html, i)
        cm = close_re.search(html, i)
        if not cm:
            return None
        if om and om.start() < cm.start():
            if om.group(0).rstrip().endswith("/>"):
                i = om.end()
                continue
            depth += 1
            i = om.end()
        else:
            depth -= 1
            if depth == 0:
                return html[start : cm.start()]
            i = cm.end()
    return None


def html_fragment_to_md(fragment: str) -> str:
    if not fragment:
        return ""
    text = re.sub(r"<script[\s\S]*?</script>", "", fragment, flags=re.I)
    text = re.sub(r"<style[\s\S]*?</style>", "", text, flags=re.I)
    # drop CTA buttons
    text = re.sub(r"<a\b[^>]*class=\"[^\"]*project-button[^\"]*\"[^>]*>[\s\S]*?</a>", "", text, flags=re.I)
    # images -> absolute markdown
    def img_repl(m):
        attrs = m.group(0)
        src_m = re.search(r'src="([^"]+)"', attrs, re.I)
        alt_m = re.search(r'alt="([^"]*)"', attrs, re.I)
        src = src_m.group(1) if src_m else ""
        alt = unescape(alt_m.group(1)) if alt_m else ""
        if src.startswith("/"):
            src = BASE + src
        return f"\n\n![{alt}]({src})\n\n"

    text = re.sub(r"<img\b[^>]*/?>", img_repl, text, flags=re.I)

    # blockquotes
    def bq_repl(m):
        inner = html_fragment_to_md(m.group(1)).strip()
        lines = [f"> {ln}" if ln else ">" for ln in inner.splitlines()]
        return "\n\n" + "\n".join(lines) + "\n\n"

    text = re.sub(r"<blockquote\b[^>]*>([\s\S]*?)</blockquote>", bq_repl, text, flags=re.I)

    # headings (strip inner tags first)
    def h_repl(level):
        def _r(m):
            title = strip_tags(m.group(1))
            if not title or EYEBROW_RE.match(title):
                return "\n\n"
            return f"\n\n{'#' * level} {title}\n\n"

        return _r

    text = re.sub(r"<h1\b[^>]*>([\s\S]*?)</h1>", h_repl(1), text, flags=re.I)
    text = re.sub(r"<h2\b[^>]*>([\s\S]*?)</h2>", h_repl(2), text, flags=re.I)
    text = re.sub(r"<h3\b[^>]*>([\s\S]*?)</h3>", h_repl(3), text, flags=re.I)

    # lists
    def li_repl(m):
        inner_html = m.group(1)
        code_m = re.search(r"<code\b[^>]*>([\s\S]*?)</code>", inner_html, re.I)
        p_m = re.search(r"<p\b[^>]*>([\s\S]*?)</p>", inner_html, re.I)
        if code_m and p_m:
            code = strip_tags(code_m.group(1))
            body = strip_tags(p_m.group(1))
            return f"\n- **{code}**：{body}" if body else f"\n- {code}"
        inner = strip_tags(inner_html)
        inner = re.sub(r"\s+", " ", inner).strip()
        return f"\n- {inner}" if inner else ""

    text = re.sub(r"<li\b[^>]*>([\s\S]*?)</li>", li_repl, text, flags=re.I)
    text = re.sub(r"</?(ul|ol)\b[^>]*>", "\n\n", text, flags=re.I)

    # paragraphs / divs that act as blocks
    text = re.sub(r"<p\b[^>]*>([\s\S]*?)</p>", lambda m: f"\n\n{strip_tags(m.group(1))}\n\n", text, flags=re.I)
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.I)
    # leftover eyebrow labels in <p> already handled; strip remaining tags
    text = re.sub(r"<[^>]+>", "", text)
    text = unescape(text)
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    # drop lone eyebrow lines
    kept = []
    for line in text.splitlines():
        if EYEBROW_RE.match(line.strip()):
            continue
        kept.append(line)
    return "\n".join(kept).strip()


def extract_series_nav(html: str) -> dict:
    """Parse prev/next from project-hub-actions."""
    nav = extract_by_class(html, "project-hub-actions", "nav") or ""
    prev_slug = prev_title = next_slug = next_title = None
    for m in re.finditer(
        r'<a\b[^>]*href="(/questions/project/[^"]+)"[^>]*>([\s\S]*?)</a>',
        nav,
        re.I,
    ):
        href = m.group(1)
        label = strip_tags(m.group(2))
        slug = href.rstrip("/").split("/")[-1]
        if "上一题" in label:
            prev_slug = slug
            prev_title = re.sub(r"^.*?上一题[：:]\s*", "", label)
            prev_title = re.sub(r"[←→\s]+$", "", prev_title).strip()
        elif "下一题" in label:
            next_slug = slug
            next_title = re.sub(r"^.*?下一题[：:]\s*", "", label)
            next_title = re.sub(r"[←→\s]+$", "", next_title).strip()
    return {
        "prev_slug": prev_slug,
        "prev_title": prev_title,
        "next_slug": next_slug,
        "next_title": next_title,
    }


def series_from_url(url: str) -> str:
    if "/project/rag/" in url:
        return "项目面试·RAG"
    if "/project/deep-research/" in url or "/project/deep_research/" in url:
        return "项目面试·Deep Research"
    if "/project/agent/" in url:
        return "项目面试·Agent"
    return "项目面试·RAG"


def parse_project_page(html: str, meta: dict) -> dict:
    title = meta.get("name") or ""
    if not title:
        h1 = re.search(r'<h1\b[^>]*class="[^"]*project-title-long[^"]*"[^>]*>([\s\S]*?)</h1>', html, re.I)
        if h1:
            title = strip_tags(h1.group(1))
    if not title:
        title = "未命名项目题"

    parts = []
    quick = extract_by_class(html, "project-quick-answer")
    if quick:
        qa_md = html_fragment_to_md(quick)
        qa_md = re.sub(r"^30\s*秒先说结论\s*\n+", "", qa_md).strip()
        if qa_md:
            parts.append("## 30 秒先说结论\n\n" + qa_md)

    body = extract_by_class(html, "project-article-body")
    if body:
        parts.append(html_fragment_to_md(body))

    follow = extract_by_id(html, "follow-ups") or extract_by_class(html, "project-follow-up-section", "section")
    if follow:
        parts.append(html_fragment_to_md(follow))

    check = extract_by_id(html, "project-checklist") or extract_by_class(html, "project-self-check", "section")
    if check:
        parts.append(html_fragment_to_md(check))

    answer = "\n\n".join(p for p in parts if p).strip()
    if not answer:
        # fallback: body only without chrome blocks
        cleaned = html
        for cls in (
            "project-question-rail",
            "project-page-toc",
            "project-page-toc-nav",
            "project-mobile-navigation",
            "project-mobile-page-toc",
            "project-mobile-question-list",
            "project-practice-index",
            "project-hub-actions",
            "project-article-breadcrumb",
            "project-article-meta",
            "project-article-provenance",
        ):
            block = extract_by_class(cleaned, cls)
            if block is not None:
                cleaned = cleaned.replace(block, "")
        answer = html_fragment_to_md(cleaned)

    url = meta.get("url") or ""
    if not url:
        can = re.search(r'<link\s+rel="canonical"\s+href="([^"]+)"', html, re.I)
        if can:
            url = can.group(1)
    slug = meta.get("slug") or (url.rstrip("/").split("/")[-1] if url else "unknown")
    nav = extract_series_nav(html)

    return {
        "id": f"proj-{slug}",
        "slug": slug,
        "title": title,
        "question": title,
        "answer": answer,
        "module": series_from_url(url),
        "url": url,
        "tags": ["项目面试"],
        "source": "项目面试题",
        "prev_slug": nav.get("prev_slug"),
        "prev_title": nav.get("prev_title"),
        "next_slug": nav.get("next_slug"),
        "next_title": nav.get("next_title"),
    }


def question_to_md(item: dict, source_kind: str) -> tuple[str, str, str, str]:
    module = item.get("module") or "未分类"
    title = (
        (item.get("tldr") or {}).get("title")
        or item.get("title")
        or item.get("question", "")[:80]
    )
    slug = item.get("slug") or item.get("id") or "unknown"
    q = item.get("question") or item.get("title") or ""
    answer = item.get("answer") or ""
    spoken = item.get("spokenAnswer") or {}
    spoken_script = spoken.get("script") or ""
    spoken_outline = spoken.get("outline") or []
    iq = item.get("interviewerQuestions") or []
    tags = item.get("tags") or []
    source = item.get("source") or ""
    if source_kind == "bank":
        url = f"{BASE}/q/{slug}"
    else:
        url = item.get("url") or f"{BASE}/questions/project"

    lines = [
        f"**专题模块**：{module}",
        f"**来源标签**：{', '.join(tags) if tags else source or '—'}",
        "",
        "## 题目",
        "",
        q.strip(),
        "",
    ]
    if answer.strip():
        lines += ["## 书面答案", "", answer.strip(), ""]
    if spoken_outline:
        lines += ["## 口语大纲", ""]
        for x in spoken_outline:
            lines.append(f"- {x}")
        lines.append("")
    if spoken_script.strip():
        lines += ["## 口语讲法", "", spoken_script.strip(), ""]
    if iq:
        lines += ["## 面试官可能这样问", ""]
        for i, row in enumerate(iq, 1):
            angle = row.get("angle") or ""
            text = row.get("text") or ""
            lines.append(f"{i}. **{angle}**：{text}")
        lines.append("")
    lines += ["## 我的笔记", "", "（在此补充自己的理解与项目经历）", ""]
    lines += [
        "---",
        "",
        f"> 个人学习摘录，版权归原作者。来源：[{title}]({url})",
        "",
    ]
    return "\n".join(lines), title, module, slug


def project_to_md(item: dict, series_map: dict[str, dict]) -> tuple[str, str, str, str]:
    module = item.get("module") or "项目面试"
    title = item.get("title") or item.get("question") or ""
    slug = item.get("slug") or "unknown"
    answer = (item.get("answer") or "").strip()
    url = item.get("url") or f"{BASE}/questions/project"

    lines = [
        f"**专题模块**：{module}",
        "",
        "## 题目",
        "",
        title.strip(),
        "",
    ]
    if answer:
        lines += ["## 书面答案", "", answer, ""]

    nav_lines = []
    prev_slug = item.get("prev_slug")
    next_slug = item.get("next_slug")
    if prev_slug:
        info = series_map.get(prev_slug) or {}
        prev_title = item.get("prev_title") or info.get("title") or prev_slug
        prev_art = info.get("art_slug") or article_slug(module, prev_slug)
        nav_lines.append(f"- 上一题：[{prev_title}](/articles/{prev_art})")
    if next_slug:
        info = series_map.get(next_slug) or {}
        next_title = item.get("next_title") or info.get("title") or next_slug
        next_art = info.get("art_slug") or article_slug(module, next_slug)
        nav_lines.append(f"- 下一题：[{next_title}](/articles/{next_art})")
    if nav_lines:
        lines += ["## 同系列导航", ""] + nav_lines + [""]

    lines += [
        "## 我的笔记",
        "",
        "（在此补充自己的理解与项目经历）",
        "",
        "---",
        "",
        f"> 个人学习摘录，版权归原作者。来源：[{title}]({url})",
        "",
    ]
    return "\n".join(lines), title, module, slug


def crawl_bank():
    RAW.mkdir(parents=True, exist_ok=True)
    MD.mkdir(parents=True, exist_ok=True)
    all_items = []
    for name in LLM_FILES:
        url = f"{BASE}/questions/llm/{name}"
        print(f"GET {url}")
        try:
            data = json.loads(fetch_text(url))
        except Exception as e:
            print(f"  skip: {e}")
            time.sleep(DELAY)
            continue
        out = RAW / name
        out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        if isinstance(data, list):
            print(f"  {len(data)} items")
            all_items.extend(data)
        elif isinstance(data, dict) and "items" in data:
            items = data["items"]
            print(f"  {len(items)} items (wrapped)")
            all_items.extend(items)
        else:
            print(f"  unknown shape: {type(data)}")
        time.sleep(DELAY)
    return all_items


def extract_project_urls(html: str) -> list[dict]:
    urls = []
    for m in re.finditer(
        r'"url"\s*:\s*"(https://www\.wushixiongai\.com/questions/project/[^"]+)"\s*,\s*"name"\s*:\s*"([^"]+)"',
        html,
    ):
        urls.append({"url": m.group(1), "name": m.group(2)})
    if not urls:
        for m in re.finditer(
            r'"name"\s*:\s*"([^"]+)"\s*,\s*"url"\s*:\s*"(https://www\.wushixiongai\.com/questions/project/[^"]+)"',
            html,
        ):
            urls.append({"url": m.group(2), "name": m.group(1)})
    seen = set()
    out = []
    for x in urls:
        if x["url"] not in seen:
            seen.add(x["url"])
            out.append(x)
    return out


def crawl_project():
    RAW.mkdir(parents=True, exist_ok=True)
    list_url = f"{BASE}/questions/project"
    print(f"GET {list_url}")
    html = fetch_text(list_url)
    (RAW / "project-index.html").write_text(html, encoding="utf-8")
    metas = extract_project_urls(html)
    print(f"  project items: {len(metas)}")
    items = []
    for i, meta in enumerate(metas, 1):
        print(f"  [{i}/{len(metas)}] {meta['url']}")
        try:
            page = fetch_text(meta["url"])
            item = parse_project_page(page, meta)
            items.append(item)
            slug = item["slug"]
            (RAW / f"project-{slug}.html").write_text(page, encoding="utf-8")
        except Exception as e:
            print(f"    fail: {e}")
        time.sleep(DELAY)
    (RAW / "project-items.json").write_text(
        json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return items


def reparse_project_offline() -> list[dict]:
    """Re-parse saved project-*.html without network."""
    RAW.mkdir(parents=True, exist_ok=True)
    items = []
    files = sorted(RAW.glob("project-*.html"))
    files = [f for f in files if f.name != "project-index.html"]
    print(f"offline reparse: {len(files)} files")
    for path in files:
        html = path.read_text(encoding="utf-8")
        can = re.search(r'<link\s+rel="canonical"\s+href="([^"]+)"', html, re.I)
        url = can.group(1) if can else ""
        slug = path.name[len("project-") : -len(".html")]
        if not url:
            # guess from slug path using series folders
            for series in ("rag", "agent", "deep-research"):
                # try from content links
                pass
            m = re.search(
                rf'href="(https://www\.wushixiongai\.com/questions/project/[^"]*{re.escape(slug)})"',
                html,
            )
            if m:
                url = m.group(1)
            else:
                # reconstruct: look for /questions/project/<series>/<slug>
                m2 = re.search(
                    rf'/questions/project/([a-z0-9-]+)/{re.escape(slug)}',
                    html,
                )
                if m2:
                    url = f"{BASE}/questions/project/{m2.group(1)}/{slug}"
        meta = {"url": url, "name": "", "slug": slug}
        item = parse_project_page(html, meta)
        items.append(item)
        print(f"  ok {item['slug']} chars={len(item.get('answer') or '')}")
    (RAW / "project-items.json").write_text(
        json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return items


def write_markdown(items: list[dict], source_kind: str) -> list[dict]:
    MD.mkdir(parents=True, exist_ok=True)
    series_map: dict[str, dict] = {}
    if source_kind == "project":
        for item in items:
            slug = item.get("slug") or ""
            module = item.get("module") or ""
            series_map[slug] = {
                "title": item.get("title") or item.get("question") or slug,
                "art_slug": article_slug(module, slug),
                "module": module,
            }

    manifest = []
    for item in items:
        if source_kind == "project":
            md, title, module, slug = project_to_md(item, series_map)
        else:
            md, title, module, slug = question_to_md(item, source_kind)
        cat_slug = slugify_module(module)
        folder = MD / cat_slug
        folder.mkdir(parents=True, exist_ok=True)
        art_slug = article_slug(module, slug)
        path = folder / f"{art_slug}.md"
        path.write_text(md, encoding="utf-8")
        summary = (item.get("question") or title or "")[:180]
        manifest.append(
            {
                "title": title[:200],
                "slug": art_slug,
                "module": module,
                "categorySlug": cat_slug,
                "categoryName": module if module.startswith("项目面试") else module,
                "summary": summary,
                "path": str(path.relative_to(ROOT)).replace("\\", "/"),
                "tags": list(
                    dict.fromkeys(
                        (item.get("tags") or [])
                        + (["题库"] if source_kind == "bank" else ["项目面试"])
                        + [module]
                    )
                ),
                "sourceUrl": item.get("url")
                or (f"{BASE}/q/{slug}" if source_kind == "bank" else None),
            }
        )
    man_path = ROOT / "data" / "manifest.json"
    man_path.parent.mkdir(parents=True, exist_ok=True)
    existing = []
    if man_path.exists():
        existing = json.loads(man_path.read_text(encoding="utf-8"))
    by_slug = {x["slug"]: x for x in existing}
    for x in manifest:
        by_slug[x["slug"]] = x
    merged = list(by_slug.values())
    man_path.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"markdown written: {len(manifest)}, manifest total: {len(merged)}")
    return manifest


def reparse_bank_offline() -> list[dict]:
    """Re-generate bank markdown from saved data/raw/*.json (no network)."""
    RAW.mkdir(parents=True, exist_ok=True)
    all_items: list[dict] = []
    for name in LLM_FILES:
        path = RAW / name
        if not path.exists():
            print(f"  missing {name}, skip")
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, list):
            items = data
        elif isinstance(data, dict) and "items" in data:
            items = data["items"]
        else:
            print(f"  unknown shape {name}")
            continue
        print(f"  {name}: {len(items)}")
        all_items.extend(items)
    print(f"offline bank total: {len(all_items)}")
    return all_items


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--offline-project",
        action="store_true",
        help="仅离线重解析 data/raw/project-*.html 并重生 Markdown",
    )
    ap.add_argument(
        "--offline-bank",
        action="store_true",
        help="仅离线用 data/raw/*.json 重生题库 Markdown（来源文末）",
    )
    ap.add_argument("--bank-only", action="store_true")
    ap.add_argument("--project-only", action="store_true", help="联网抓取项目面试题")
    args = ap.parse_args()

    if args.offline_project:
        print("=== offline reparse project ===")
        proj = reparse_project_offline()
        write_markdown(proj, "project")
        print("done")
        return

    if args.offline_bank:
        print("=== offline reparse bank ===")
        bank = reparse_bank_offline()
        write_markdown(bank, "bank")
        print("done")
        return

    if not args.project_only:
        print("=== crawl question bank ===")
        bank = crawl_bank()
        write_markdown(bank, "bank")
    if not args.bank_only:
        print("=== crawl project interview ===")
        proj = crawl_project()
        write_markdown(proj, "project")
    print("done")


if __name__ == "__main__":
    main()
