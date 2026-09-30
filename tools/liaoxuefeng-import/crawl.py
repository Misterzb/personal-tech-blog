"""
廖雪峰教程 — 本地学习抓取脚本
个人非商业学习整理；CC BY-NC-SA 4.0，发布请自行确认合规。
"""
from __future__ import annotations

import argparse
import html as html_lib
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://liaoxuefeng.com"
LICENSE_URL = "https://liaoxuefeng.com/pages/license/index.html"
UA = "PersonalLocalStudyBot/1.0 (+local non-commercial study; CC-BY-NC-SA attribution)"
DELAY = 1.0
ROOT = Path(__file__).resolve().parent
RAW = ROOT / "data" / "raw"
MD = ROOT / "data" / "md"
MANIFEST = ROOT / "data" / "manifest.json"

# book_slug -> (categoryName, categorySlug, tagName, tagSlug)
SERIES = {
    "java": ("Java教程", "lxf-java", "Java", "java"),
    "python": ("Python教程", "lxf-python", "Python", "python"),
    "javascript": ("JavaScript教程", "lxf-javascript", "JavaScript", "javascript"),
    "sql": ("SQL教程", "lxf-sql", "SQL", "sql"),
    "summerframework": ("手写Spring", "lxf-spring", "Spring", "spring"),
    "jerrymouse": ("手写Tomcat", "lxf-tomcat", "Tomcat", "tomcat"),
}

EXCLUDE = {"blockchain", "git", "makefile"}


def fetch_text(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/html,*/*"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read().decode("utf-8", errors="replace")


def unescape(text: str) -> str:
    return html_lib.unescape(text or "")


def strip_tags(fragment: str) -> str:
    t = re.sub(r"<br\s*/?>", "\n", fragment, flags=re.I)
    t = re.sub(r"<[^>]+>", "", t)
    return unescape(t).strip()


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


def abs_url(href: str, page_url: str) -> str:
    if not href:
        return ""
    if href.startswith("http://") or href.startswith("https://"):
        return href
    return urllib.parse.urljoin(page_url, href)


def normalize_path(href: str) -> str:
    u = urllib.parse.urlparse(href)
    path = u.path or ""
    if path.endswith("/"):
        path = path + "index.html"
    return path


def page_key_from_path(path: str, book: str) -> str:
    prefix = f"/books/{book}/"
    if not path.startswith(prefix):
        return "unknown"
    rest = path[len(prefix) :]
    rest = re.sub(r"/index\.html$", "", rest).strip("/")
    if not rest or rest == "index.html":
        return "root"
    return rest.replace("/", "-")


def article_slug(category_slug: str, page_key: str) -> str:
    s = f"{category_slug}-{page_key}"
    s = re.sub(r"[^a-zA-Z0-9\-]+", "-", s).strip("-").lower()
    return s[:120]


def format_marker(marker: str) -> str:
    return (marker or "").strip().rstrip(".")


def parse_toc(html: str, book: str) -> list[dict]:
    index = extract_by_id(html, "gsi-index")
    if not index:
        return []

    nodes: list[dict] = []
    item_depth = 0
    div_kinds: list[str] = []
    title_at_depth: dict[int, str] = {}
    i = 0
    n = len(index)
    while i < n:
        if index.startswith("</div>", i):
            kind = div_kinds.pop() if div_kinds else "other"
            if kind == "children":
                item_depth = max(0, item_depth - 1)
            i += 6
            continue
        if index[i] != "<":
            i += 1
            continue
        if index.startswith("<div", i):
            end = index.find(">", i)
            if end < 0:
                break
            tag = index[i : end + 1]
            if "gsc-index-item-children" in tag:
                div_kinds.append("children")
                item_depth += 1
                i = end + 1
                continue
            if "gsc-index-item" in tag:
                div_kinds.append("item")
                rest = index[end + 1 :]
                am = re.match(
                    r'\s*<a[^>]+href="([^"]+)"[^>]*>'
                    r"([\s\S]*?)</a>",
                    rest,
                    re.I,
                )
                if am:
                    href = am.group(1)
                    body = am.group(2)
                    mm = re.search(
                        r'class="gsc-index-item-marker"[^>]*>\s*([^<]+)', body, re.I
                    )
                    tm = re.search(
                        r'class="gsc-index-item-title"[^>]*>\s*([^<]+)', body, re.I
                    )
                    marker = unescape(mm.group(1)).strip() if mm else ""
                    title = unescape(tm.group(1)).strip() if tm else strip_tags(body)
                    path = normalize_path(href)
                    skip = path.endswith(f"/books/{book}/index.html") or path.rstrip(
                        "/"
                    ) == f"/books/{book}"
                    if (not skip) and f"/books/{book}/" in path:
                        ancestors = [
                            title_at_depth[d]
                            for d in range(item_depth)
                            if d in title_at_depth
                        ]
                        full_url = href if href.startswith("http") else BASE + path
                        nodes.append(
                            {
                                "href": full_url.split("#")[0],
                                "path": path,
                                "marker": marker,
                                "title": title,
                                "depth": item_depth,
                                "ancestors": ancestors,
                            }
                        )
                        title_at_depth[item_depth] = title
                        for d in list(title_at_depth.keys()):
                            if d > item_depth:
                                del title_at_depth[d]
                    i = end + 1 + am.end()
                    continue
                i = end + 1
                continue
            div_kinds.append("other")
            i = end + 1
            continue
        i += 1

    seen: set[str] = set()
    out: list[dict] = []
    for node in nodes:
        if node["path"] in seen:
            continue
        seen.add(node["path"])
        out.append(node)
    return out


def _remove_balanced_div_at(html: str, start: int) -> str:
    """Remove a <div…>…</div> tree starting at start."""
    if start < 0 or not html[start:].lower().startswith("<div"):
        return html
    open_re = re.compile(r"<div\b[^>]*>", re.I)
    close_re = re.compile(r"</div\s*>", re.I)
    first = open_re.match(html, start)
    if not first:
        return html
    depth = 1
    i = first.end()
    while i < len(html) and depth > 0:
        om = open_re.search(html, i)
        cm = close_re.search(html, i)
        if not cm:
            return html
        if om and om.start() < cm.start():
            depth += 1
            i = om.end()
        else:
            depth -= 1
            i = cm.end()
            if depth == 0:
                return html[:start] + html[i:]
    return html


def strip_author_card(content: str) -> str:
    # Prefer removing the top flex author card that links to weibo/qlogo
    for needle in ("weibo.com/liaoxuefeng", "thirdqq.qlogo.cn"):
        pos = content.lower().find(needle.lower())
        if pos < 0:
            continue
        # walk back to nearest <div style="display:flex
        window = content[max(0, pos - 800) : pos]
        m = None
        for mm in re.finditer(r'<div\s+style="display:flex[^"]*"', window, re.I):
            m = mm
        if m:
            start = max(0, pos - 800) + m.start()
            content = _remove_balanced_div_at(content, start)
            break
    return content


def clean_md_leading_noise(md: str) -> str:
    """Drop leftover author social links / bio lines at the top of body."""
    lines = md.splitlines()
    i = 0
    social_re = re.compile(
        r"^(https?://)?(weibo\.com|github\.com/michaelliao|zhihu\.com/people|twitter\.com)/",
        re.I,
    )
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        # markdown links to social
        if re.match(r"^\[.*\]\(https?://(weibo|github|zhihu|twitter)\.", line, re.I):
            i += 1
            continue
        if social_re.search(line):
            i += 1
            continue
        if "资深软件开发工程师" in line or line in ("廖雪峰",):
            i += 1
            continue
        break
    return "\n".join(lines[i:]).strip()

def html_fragment_to_md(fragment: str, page_url: str) -> str:
    if not fragment:
        return ""
    text = re.sub(r"<script[\s\S]*?</script>", "", fragment, flags=re.I)
    text = re.sub(r"<style[\s\S]*?</style>", "", text, flags=re.I)
    text = strip_author_card(text)
    text = re.sub(r"<button\b[^>]*>[\s\S]*?</button>", "", text, flags=re.I)

    def pre_repl(m):
        inner = m.group(1)
        lang = ""
        lm = re.search(r'class="[^"]*language-([a-zA-Z0-9_+-]+)', inner, re.I)
        if lm:
            lang = lm.group(1)
        code = re.sub(r"<[^>]+>", "", inner)
        code = unescape(code)
        if code.startswith("\n"):
            code = code[1:]
        code = code.rstrip() + "\n"
        return f"\n\n```{lang}\n{code}```\n\n"

    text = re.sub(r"<pre\b[^>]*>([\s\S]*?)</pre>", pre_repl, text, flags=re.I)

    def img_repl(m):
        attrs = m.group(0)
        src_m = re.search(r'src="([^"]+)"', attrs, re.I)
        alt_m = re.search(r'alt="([^"]*)"', attrs, re.I)
        src = src_m.group(1) if src_m else ""
        alt = unescape(alt_m.group(1)) if alt_m else ""
        if "qlogo.cn" in src or "thirdqq" in src:
            return ""
        src = abs_url(src, page_url)
        return f"\n\n![{alt}]({src})\n\n"

    text = re.sub(r"<img\b[^>]*/?>", img_repl, text, flags=re.I)

    def bq_repl(m):
        inner = html_fragment_to_md(m.group(1), page_url).strip()
        lines = [f"> {ln}" if ln else ">" for ln in inner.splitlines()]
        return "\n\n" + "\n".join(lines) + "\n\n"

    text = re.sub(r"<blockquote\b[^>]*>([\s\S]*?)</blockquote>", bq_repl, text, flags=re.I)

    def h_repl(level):
        def _r(m):
            title = strip_tags(m.group(1))
            if not title:
                return "\n\n"
            return f"\n\n{'#' * level} {title}\n\n"

        return _r

    for lv in range(1, 7):
        text = re.sub(rf"<h{lv}\b[^>]*>([\s\S]*?)</h{lv}>", h_repl(lv), text, flags=re.I)

    def li_repl(m):
        inner = re.sub(r"\s+", " ", strip_tags(m.group(1))).strip()
        return f"\n- {inner}" if inner else ""

    text = re.sub(r"<li\b[^>]*>([\s\S]*?)</li>", li_repl, text, flags=re.I)
    text = re.sub(r"</?(ul|ol)\b[^>]*>", "\n\n", text, flags=re.I)

    def a_repl(m):
        href = abs_url(m.group(1), page_url)
        label = strip_tags(m.group(2)) or href
        return f"[{label}]({href})"

    text = re.sub(r'<a\b[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>', a_repl, text, flags=re.I)
    text = re.sub(
        r"<p\b[^>]*>([\s\S]*?)</p>",
        lambda m: f"\n\n{strip_tags(m.group(1))}\n\n",
        text,
        flags=re.I,
    )
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.I)
    text = re.sub(
        r"<code\b[^>]*>([\s\S]*?)</code>",
        lambda m: f"`{strip_tags(m.group(1))}`",
        text,
        flags=re.I,
    )
    text = re.sub(r"<[^>]+>", "", text)
    text = unescape(text)
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text


def chapter_title_from_html(html: str) -> str:
    block = extract_by_id(html, "gsi-chapter-title") or ""
    m = re.search(r"<h1\b[^>]*>([\s\S]*?)</h1>", block, re.I)
    return strip_tags(m.group(1)) if m else ""


def build_display_title(marker: str, toc_title: str, h1: str) -> str:
    name = toc_title or h1 or "未命名"
    mk = format_marker(marker)
    return f"{mk} {name}" if mk else name


def make_md(
    *,
    display_title: str,
    book_name: str,
    ancestors: list[str],
    body_md: str,
    source_url: str,
    prev_item: dict | None,
    next_item: dict | None,
) -> str:
    crumbs = " › ".join([book_name] + list(ancestors))
    # 不在正文再写一级标题：详情页已有 h1，避免标题出现两次
    body = body_md.strip()
    body = re.sub(
        rf"^#\s+{re.escape(display_title)}\s*\n+",
        "",
        body,
        count=1,
    )
    lines = [
        f"**{crumbs}**",
        "",
        body,
        "",
    ]
    nav = []
    if prev_item:
        nav.append(f"[← {prev_item['display_title']}](/articles/{prev_item['slug']})")
    if next_item:
        nav.append(f"[{next_item['display_title']} →](/articles/{next_item['slug']})")
    if nav:
        lines.extend(["---", "", " | ".join(nav), ""])
    lines.extend(
        [
            "---",
            "",
            "## 我的笔记",
            "",
            "（在此补充自己的理解与项目经历）",
            "",
            "---",
            "",
            f"> 个人学习摘录（非商业）。原作：[廖雪峰](https://liaoxuefeng.com/)；"
            f"来源：[{display_title}]({source_url})；"
            f"许可：[CC BY-NC-SA 4.0]({LICENSE_URL})。",
            "",
        ]
    )
    return "\n".join(lines)


def raw_path_for(book: str, path: str) -> Path:
    rel = path
    if rel.startswith("/books/"):
        rel = rel[len("/books/") :]
    return RAW / rel


def load_or_fetch(url: str, dest: Path, offline: bool) -> str:
    if dest.exists():
        return dest.read_text(encoding="utf-8")
    if offline:
        raise FileNotFoundError(f"offline missing: {dest}")
    html = fetch_text(url)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(html, encoding="utf-8")
    time.sleep(DELAY)
    return html


def crawl_series(book: str, offline: bool = False) -> list[dict]:
    if book not in SERIES:
        raise ValueError(f"unknown series: {book}")
    cat_name, cat_slug, tag_name, _tag_slug = SERIES[book]
    seed_path = f"/books/{book}/introduction/index.html"
    seed_url = BASE + seed_path
    seed_raw = raw_path_for(book, seed_path)

    print(f"[{book}] load TOC from {seed_url}")
    seed_html = load_or_fetch(seed_url, seed_raw, offline)
    toc = parse_toc(seed_html, book)
    if not toc:
        raise RuntimeError(f"empty TOC for {book}")
    print(f"[{book}] TOC pages: {len(toc)}")

    pages: list[dict] = []
    for idx, node in enumerate(toc, 1):
        url = node["href"]
        path = node["path"]
        dest = raw_path_for(book, path)
        try:
            html = load_or_fetch(url, dest, offline)
        except Exception as e:
            print(f"  FAIL fetch {path}: {e}")
            continue
        h1 = chapter_title_from_html(html)
        content = extract_by_id(html, "gsi-chapter-content") or ""
        body_md = clean_md_leading_noise(html_fragment_to_md(content, url))
        if not body_md.strip():
            print(f"  SKIP empty body {path}")
            continue
        page_key = page_key_from_path(path, book)
        slug = article_slug(cat_slug, page_key)
        display_title = build_display_title(node["marker"], node["title"], h1)
        # 摘要去掉可能的标题行，避免列表/详情摘要像又套一层标题
        summary_src = re.sub(rf"^#\s+{re.escape(display_title)}\s*", "", body_md).strip()
        summary = re.sub(r"\s+", " ", summary_src)[:180]
        pages.append(
            {
                "categoryName": cat_name,
                "categorySlug": cat_slug,
                "title": display_title,
                "display_title": display_title,
                "slug": slug,
                "summary": summary,
                "sourceUrl": url,
                "ancestors": node["ancestors"],
                "body_md": body_md,
                "tags": [tag_name],
            }
        )
        if idx % 20 == 0 or idx == len(toc):
            print(f"  [{idx}/{len(toc)}] {display_title}")

    out_dir = MD / cat_slug
    out_dir.mkdir(parents=True, exist_ok=True)
    manifest_items = []
    for i, page in enumerate(pages):
        prev_item = pages[i - 1] if i > 0 else None
        next_item = pages[i + 1] if i + 1 < len(pages) else None
        md = make_md(
            display_title=page["display_title"],
            book_name=cat_name,
            ancestors=page["ancestors"],
            body_md=page["body_md"],
            source_url=page["sourceUrl"],
            prev_item=prev_item,
            next_item=next_item,
        )
        rel = f"data/md/{cat_slug}/{page['slug']}.md"
        (ROOT / rel).write_text(md, encoding="utf-8")
        manifest_items.append(
            {
                "title": page["title"],
                "slug": page["slug"],
                "module": cat_name,
                "categorySlug": cat_slug,
                "categoryName": cat_name,
                "summary": page["summary"],
                "path": rel.replace("\\", "/"),
                "tags": page["tags"],
                "sourceUrl": page["sourceUrl"],
                "sortOrder": i + 1,
            }
        )
    return manifest_items


def merge_manifest(new_items: list[dict]) -> list[dict]:
    existing: list[dict] = []
    if MANIFEST.exists():
        existing = json.loads(MANIFEST.read_text(encoding="utf-8"))
    by_slug = {x["slug"]: x for x in existing}
    for item in new_items:
        by_slug[item["slug"]] = item
    cat_order = {t[1]: i for i, t in enumerate(SERIES.values())}
    merged = list(by_slug.values())
    merged.sort(
        key=lambda x: (
            cat_order.get(x.get("categorySlug") or "", 99),
            x.get("sortOrder") or 0,
            x.get("slug") or "",
        )
    )
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
    return merged


def main():
    global DELAY
    ap = argparse.ArgumentParser()
    ap.add_argument(
        "--series",
        default="",
        help="只抓一个系列：java/python/javascript/sql/summerframework/jerrymouse",
    )
    ap.add_argument("--offline", action="store_true", help="不联网，仅用本地 raw 重生成")
    ap.add_argument("--delay", type=float, default=DELAY)
    args = ap.parse_args()
    DELAY = args.delay

    books = [args.series] if args.series else list(SERIES.keys())
    for b in books:
        if b in EXCLUDE:
            print(f"skip excluded: {b}")
            continue
        if b not in SERIES:
            raise SystemExit(f"unknown series {b}; choose from {list(SERIES)}")
        items = crawl_series(b, offline=args.offline)
        merged = merge_manifest(items)
        print(f"[{b}] wrote {len(items)} pages; manifest total {len(merged)}")
    print("done")


if __name__ == "__main__":
    main()
