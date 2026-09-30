"""
整理廖雪峰导入标签：按专题绑定技术标签并重挂文章。

用法:
  python sync_tags.py --base http://localhost:8080 --user admin --password admin123
"""
from __future__ import annotations

import argparse
import json
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MANIFEST = ROOT / "data" / "manifest.json"

TOPIC_TAG_BY_CATEGORY: dict[str, tuple[str, str]] = {
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
        raise RuntimeError(res.get("message"))
    t = res["data"]
    by_slug[slug] = t
    by_name[name] = t
    return t["id"]


def load_articles(base: str, token: str) -> list[dict]:
    out = []
    page = 1
    while page <= 500:
        res = http_json("GET", f"{base}/api/admin/articles?page={page}&size=100", token=token)
        data = res.get("data") or {}
        records = data.get("records") or []
        out.extend(records)
        total = data.get("total") or 0
        if not records or page * 100 >= total:
            break
        page += 1
    return out


def get_article(base: str, token: str, aid: int) -> dict:
    res = http_json("GET", f"{base}/api/admin/articles/{aid}", token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    return res["data"]


def save_article(base: str, token: str, payload: dict):
    res = http_json("POST", f"{base}/api/admin/articles", payload, token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    return res["data"]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--user", default="admin")
    ap.add_argument("--password", default="admin123")
    ap.add_argument("--status", type=int, default=-1, help=">=0 时强制写 status；-1 保持原状")
    ap.add_argument("--sleep", type=float, default=0.05)
    args = ap.parse_args()

    if not MANIFEST.exists():
        raise SystemExit("缺少 data/manifest.json")

    items = json.loads(MANIFEST.read_text(encoding="utf-8"))
    by_slug = {x["slug"]: x for x in items if (x.get("categorySlug") or "").startswith("lxf-")}
    print(f"manifest lxf articles: {len(by_slug)}")

    token = login(args.base, args.user, args.password)
    tag_cache = {"by_slug": {}, "by_name": {}}
    raw_tags = list_tags(args.base, token)
    tag_cache["by_slug"] = {t["slug"]: t for t in raw_tags if t.get("slug")}
    tag_cache["by_name"] = {t["name"]: t for t in raw_tags if t.get("name")}

    articles = load_articles(args.base, token)
    ok = 0
    for a in articles:
        slug = a.get("slug") or ""
        if slug not in by_slug:
            continue
        meta = by_slug[slug]
        cat = meta.get("categorySlug") or ""
        topic = TOPIC_TAG_BY_CATEGORY.get(cat)
        if not topic:
            continue
        tag_id = ensure_tag(args.base, token, topic[0], topic[1], tag_cache)
        detail = get_article(args.base, token, a["id"])
        status = detail.get("status", 0) if args.status < 0 else args.status
        payload = {
            "id": detail["id"],
            "title": detail["title"],
            "slug": detail["slug"],
            "summary": detail.get("summary") or "",
            "contentMd": detail.get("contentMd") or "",
            "status": status,
            "categoryId": detail.get("categoryId"),
            "tagIds": [tag_id],
            "seoTitle": detail.get("seoTitle") or detail["title"],
            "seoDescription": detail.get("seoDescription") or "",
            "isTop": detail.get("isTop") or False,
        }
        try:
            save_article(args.base, token, payload)
            ok += 1
            print(f"ok {slug} -> {topic[0]}")
        except Exception as e:
            print(f"FAIL {slug}: {e}")
        time.sleep(args.sleep)
    print(f"done: {ok}")


if __name__ == "__main__":
    main()
