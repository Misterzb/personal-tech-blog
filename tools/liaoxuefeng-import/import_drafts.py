"""
将 data/manifest.json 中的 Markdown 幂等导入博客文章。
用法:
  python import_drafts.py --base http://localhost:8080 --user admin --password admin123
  python import_drafts.py --module lxf-sql
  python import_drafts.py --prefix lxf --status 0
"""
from __future__ import annotations

import argparse
import json
import re
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "data" / "manifest.json"

TOPIC_TAG_BY_CATEGORY = {
    "lxf-java": ("Java", "java"),
    "lxf-python": ("Python", "python"),
    "lxf-javascript": ("JavaScript", "javascript"),
    "lxf-sql": ("SQL", "sql"),
    "lxf-spring": ("Spring", "spring"),
    "lxf-tomcat": ("Tomcat", "tomcat"),
}


def http_json(method: str, url: str, data=None, token: str | None = None):
    body = None
    headers = {"Content-Type": "application/json; charset=utf-8", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> {e.code}: {err}") from e


def login(base: str, user: str, password: str) -> str:
    res = http_json("POST", f"{base}/api/admin/auth/login", {"username": user, "password": password})
    if res.get("code") != 0:
        raise RuntimeError(res.get("message") or "login failed")
    return res["data"]["token"]


def list_categories(base: str, token: str) -> dict[str, dict]:
    res = http_json("GET", f"{base}/api/admin/categories", token=token)
    return {c["slug"]: c for c in (res.get("data") or [])}


def upsert_category(base: str, token: str, name: str, slug: str, sort_order: int) -> dict:
    cats = list_categories(base, token)
    payload = {
        "name": name,
        "slug": slug,
        "description": f"本地学习专题：{name}（CC BY-NC-SA 摘录草稿，勿商用）",
        "sortOrder": sort_order,
    }
    if slug in cats:
        payload["id"] = cats[slug]["id"]
    res = http_json("POST", f"{base}/api/admin/categories", payload, token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    return res["data"]


def list_tags(base: str, token: str) -> list[dict]:
    res = http_json("GET", f"{base}/api/admin/tags", token=token)
    return res.get("data") or []


def ensure_tag(base: str, token: str, name: str, slug: str, cache: dict) -> int:
    by_slug = cache["by_slug"]
    by_name = cache["by_name"]
    if slug in by_slug:
        return by_slug[slug]["id"]
    if name in by_name:
        return by_name[name]["id"]
    res = http_json("POST", f"{base}/api/admin/tags", {"name": name, "slug": slug}, token=token)
    if res.get("code") != 0:
        tags = list_tags(base, token)
        cache["by_slug"] = {t["slug"]: t for t in tags if t.get("slug")}
        cache["by_name"] = {t["name"]: t for t in tags if t.get("name")}
        if slug in cache["by_slug"]:
            return cache["by_slug"][slug]["id"]
        if name in cache["by_name"]:
            return cache["by_name"][name]["id"]
        raise RuntimeError(res.get("message"))
    t = res["data"]
    by_slug[slug] = t
    by_name[name] = t
    return t["id"]


def tags_for_item(category_slug: str, item_tags: list | None = None) -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    topic = TOPIC_TAG_BY_CATEGORY.get(category_slug)
    if topic:
        pairs.append(topic)
    elif item_tags:
        for name in item_tags:
            name = (name or "").strip()
            if not name:
                continue
            slug = re.sub(r"[^a-zA-Z0-9\u4e00-\u9fff]+", "-", name).strip("-").lower() or "tag"
            pairs.append((name, slug))
    seen = set()
    out = []
    for n, s in pairs:
        if s in seen:
            continue
        seen.add(s)
        out.append((n, s))
    return out


def load_slug_map(base: str, token: str) -> dict[str, int]:
    mapping: dict[str, int] = {}
    page = 1
    while page <= 500:
        res = http_json("GET", f"{base}/api/admin/articles?page={page}&size=100", token=token)
        data = res.get("data") or {}
        records = data.get("records") or []
        for a in records:
            if a.get("slug"):
                mapping[a["slug"]] = a["id"]
        total = data.get("total") or 0
        if not records or page * 100 >= total:
            break
        page += 1
    return mapping


def resolve_existing_id(slug: str, slug_map: dict[str, int]) -> int | None:
    """精确匹配；否则匹配 slug-1 / slug-2（历史去重产生的后缀）。"""
    if slug in slug_map:
        return slug_map[slug]
    for n in range(1, 10):
        alt = f"{slug}-{n}"
        if alt in slug_map:
            return slug_map[alt]
    return None


def import_one(
    base: str,
    token: str,
    item: dict,
    cat_id: int,
    tag_ids: list[int],
    slug_map: dict[str, int],
    status: int = 0,
):
    md_path = ROOT / item["path"]
    content = md_path.read_text(encoding="utf-8")
    payload = {
        "title": item["title"],
        "slug": item["slug"],
        "summary": item.get("summary") or item["title"],
        "contentMd": content,
        "status": status,
        "categoryId": cat_id,
        "tagIds": tag_ids,
        "seoTitle": item["title"],
        "seoDescription": (item.get("summary") or "")[:200],
        "isTop": False,
        "sortOrder": int(item.get("sortOrder") or 0),
    }
    existing_id = resolve_existing_id(item["slug"], slug_map)
    if existing_id is not None:
        payload["id"] = existing_id
    res = http_json("POST", f"{base}/api/admin/articles", payload, token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    aid = res["data"]["id"]
    # 以规范 slug 为准写入映射，并清掉 -N 别名
    slug_map[item["slug"]] = aid
    for n in range(1, 10):
        slug_map.pop(f"{item['slug']}-{n}", None)
    return aid


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--user", default="admin")
    ap.add_argument("--password", default="admin123")
    ap.add_argument("--module", default="", help="只导入 categorySlug，如 lxf-sql")
    ap.add_argument("--prefix", default="", help="categorySlug 前缀，如 lxf")
    ap.add_argument("--status", type=int, default=0, choices=[0, 1], help="0=草稿 1=发布")
    ap.add_argument("--limit", type=int, default=0, help="最多导入 N 篇，0=全部")
    ap.add_argument("--sleep", type=float, default=0.05)
    args = ap.parse_args()

    if not MANIFEST.exists():
        raise SystemExit("缺少 data/manifest.json，请先运行 crawl.py")

    items = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if args.module:
        items = [x for x in items if x.get("categorySlug") == args.module]
    if args.prefix:
        items = [x for x in items if (x.get("categorySlug") or "").startswith(args.prefix)]
    if args.limit and args.limit > 0:
        items = items[: args.limit]
    print(f"importing {len(items)} articles with status={args.status}")

    token = login(args.base, args.user, args.password)
    print("loading existing article slug map...")
    slug_map = load_slug_map(args.base, token)
    print(f"existing articles: {len(slug_map)}")

    modules = []
    seen = set()
    for x in items:
        key = x["categorySlug"]
        if key not in seen:
            seen.add(key)
            modules.append((x["categoryName"], key))

    cat_ids = {}
    for i, (name, slug) in enumerate(modules, start=200):
        c = upsert_category(args.base, token, name, slug, i)
        cat_ids[slug] = c["id"]
        print(f"category {name} -> {c['id']}")

    ok = 0
    tag_cache = {"by_slug": {}, "by_name": {}}
    raw_tags = list_tags(args.base, token)
    tag_cache["by_slug"] = {t["slug"]: t for t in raw_tags if t.get("slug")}
    tag_cache["by_name"] = {t["name"]: t for t in raw_tags if t.get("name")}
    for idx, item in enumerate(items, 1):
        pairs = tags_for_item(item.get("categorySlug") or "", item.get("tags"))
        tag_ids = [ensure_tag(args.base, token, n, s, tag_cache) for n, s in pairs]
        try:
            aid = import_one(
                args.base,
                token,
                item,
                cat_ids[item["categorySlug"]],
                tag_ids,
                slug_map,
                status=args.status,
            )
            ok += 1
            print(f"[{idx}/{len(items)}] ok id={aid} {item['slug']} tags={[p[0] for p in pairs]}")
        except Exception as e:
            print(f"[{idx}/{len(items)}] FAIL {item['slug']}: {e}")
        time.sleep(args.sleep)
    print(f"done: {ok}/{len(items)}")


if __name__ == "__main__":
    main()
