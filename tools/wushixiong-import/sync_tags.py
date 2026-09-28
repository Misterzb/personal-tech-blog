"""
整理标签体系：按专题绑定稳定标签，重挂文章关联，清理空壳/乱 slug 标签。

用法:
  python sync_tags.py --base http://localhost:8080 --user admin --password admin123
  python sync_tags.py --status 1   # 重挂时保持/设为发布
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

# categorySlug -> (展示名, 稳定 slug)
TOPIC_TAG_BY_CATEGORY: dict[str, tuple[str, str]] = {
    "study-rag": ("RAG", "rag"),
    "study-vector": ("向量检索", "vector"),
    "study-rerank": ("重排优化", "rerank"),
    "study-doc": ("文档处理", "document"),
    "study-kg": ("知识图谱", "knowledge-graph"),
    "study-multimodal": ("多模态", "multimodal"),
    "study-agent": ("Agent", "agent"),
    "study-prompt": ("Prompt", "prompt"),
    "study-finetune": ("模型微调", "finetune"),
    "study-eval": ("评估监控", "evaluation"),
    "study-infer": ("推理优化", "inference"),
    "study-train": ("模型训练", "training"),
    "study-arch": ("模型架构", "architecture"),
    "study-rlhf": ("RLHF", "rlhf"),
    "study-experience": ("项目经历", "experience"),
    "study-proj-rag": ("项目面试", "project-interview"),
    "study-proj-agent": ("项目面试", "project-interview"),
    "study-proj-deep-research": ("项目面试", "project-interview"),
}

TYPE_BANK = ("面试题", "interview")
TYPE_PROJECT = ("项目面试", "project-interview")

# 系统示例文章可保留的标签（清理时不删）
KEEP_EXTRA_SLUGS = {"spring-boot", "vue"}


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
    """按 slug 优先复用；名称不一致则更新。"""
    by_slug = cache["by_slug"]
    by_name = cache["by_name"]
    if slug in by_slug:
        t = by_slug[slug]
        if t.get("name") != name:
            payload = {"id": t["id"], "name": name, "slug": slug}
            res = http_json("POST", f"{base}/api/admin/tags", payload, token=token)
            if res.get("code") != 0:
                raise RuntimeError(res.get("message"))
            t = res["data"]
            by_slug[slug] = t
            by_name[name] = t
        return t["id"]
    if name in by_name:
        t = by_name[name]
        # 旧中文标签可能是乱 slug，统一改成稳定 slug（若被占用则只复用 id）
        if t.get("slug") != slug and slug not in by_slug:
            payload = {"id": t["id"], "name": name, "slug": slug}
            res = http_json("POST", f"{base}/api/admin/tags", payload, token=token)
            if res.get("code") == 0:
                t = res["data"]
                by_slug[slug] = t
                by_name[name] = t
        return t["id"]
    res = http_json("POST", f"{base}/api/admin/tags", {"name": name, "slug": slug}, token=token)
    if res.get("code") != 0:
        # 刷新后按 slug/name 再试
        refresh_tag_cache(base, token, cache)
        if slug in cache["by_slug"]:
            return cache["by_slug"][slug]["id"]
        if name in cache["by_name"]:
            return cache["by_name"][name]["id"]
        raise RuntimeError(res.get("message"))
    t = res["data"]
    by_slug[slug] = t
    by_name[name] = t
    return t["id"]


def refresh_tag_cache(base: str, token: str, cache: dict):
    tags = list_tags(base, token)
    cache["by_slug"] = {t["slug"]: t for t in tags if t.get("slug")}
    cache["by_name"] = {t["name"]: t for t in tags if t.get("name")}
    cache["all"] = tags


def tags_for_category(category_slug: str) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    topic = TOPIC_TAG_BY_CATEGORY.get(category_slug)
    if topic:
        out.append(topic)
    if (category_slug or "").startswith("study-proj"):
        # 项目面试专题：主题标签已是 project-interview，不再重复挂「面试题」
        if not topic or topic[1] != TYPE_PROJECT[1]:
            out.append(TYPE_PROJECT)
    elif (category_slug or "").startswith("study-"):
        out.append(TYPE_BANK)
    # 去重保序
    seen = set()
    uniq = []
    for name, slug in out:
        if slug in seen:
            continue
        seen.add(slug)
        uniq.append((name, slug))
    return uniq


def load_slug_map(base: str, token: str) -> dict[str, dict]:
    mapping: dict[str, dict] = {}
    page = 1
    while page <= 500:
        res = http_json(
            "GET",
            f"{base}/api/admin/articles?page={page}&size=100",
            token=token,
        )
        data = res.get("data") or {}
        records = data.get("records") or []
        for a in records:
            if a.get("slug"):
                mapping[a["slug"]] = a
        total = data.get("total") or 0
        if not records or page * 100 >= total:
            break
        page += 1
    return mapping


def retag_one(
    base: str,
    token: str,
    item: dict,
    article_meta: dict,
    tag_ids: list[int],
    status: int | None,
):
    detail = http_json("GET", f"{base}/api/admin/articles/{article_meta['id']}", token=token)
    if detail.get("code") != 0:
        raise RuntimeError(detail.get("message"))
    a = detail["data"]
    # 后台详情可能不含 contentMd：回退读本地 md
    content = a.get("contentMd")
    if not content:
        md_path = ROOT / item["path"]
        content = md_path.read_text(encoding="utf-8")
    payload = {
        "id": a["id"],
        "title": a.get("title") or item["title"],
        "slug": a.get("slug") or item["slug"],
        "summary": a.get("summary") or item.get("summary") or item["title"],
        "contentMd": content,
        "status": status if status is not None else a.get("status", 1),
        "categoryId": a.get("categoryId"),
        "tagIds": tag_ids,
        "seoTitle": a.get("seoTitle") or a.get("title"),
        "seoDescription": a.get("seoDescription") or (a.get("summary") or "")[:200],
        "isTop": bool(a.get("isTop")),
        "cover": a.get("cover"),
    }
    res = http_json("POST", f"{base}/api/admin/articles", payload, token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    return res["data"]["id"]


def prune_tags(base: str, token: str, keep_slugs: set[str]):
    tags = list_tags(base, token)
    deleted = 0
    for t in tags:
        slug = t.get("slug") or ""
        if slug in keep_slugs:
            continue
        try:
            http_json("DELETE", f"{base}/api/admin/tags/{t['id']}", token=token)
            deleted += 1
            print(f"  deleted tag id={t['id']} slug={slug} name={t.get('name')}")
        except Exception as e:
            print(f"  delete fail {slug}: {e}")
    return deleted


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8080")
    ap.add_argument("--user", default="admin")
    ap.add_argument("--password", default="admin123")
    ap.add_argument("--status", type=int, default=None, choices=[0, 1], help="可选：重挂时覆盖发布状态")
    ap.add_argument("--sleep", type=float, default=0.03)
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--no-prune", action="store_true", help="不删除多余标签")
    args = ap.parse_args()

    if not MANIFEST.exists():
        raise SystemExit("缺少 data/manifest.json")

    items = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if args.limit and args.limit > 0:
        items = items[: args.limit]

    token = login(args.base, args.user, args.password)
    cache = {"by_slug": {}, "by_name": {}, "all": []}
    refresh_tag_cache(args.base, token, cache)

    # 预创建全部会用到的标签
    needed: dict[str, str] = {}
    for name, slug in [TYPE_BANK, TYPE_PROJECT, *TOPIC_TAG_BY_CATEGORY.values()]:
        needed[slug] = name
    for slug, name in needed.items():
        tid = ensure_tag(args.base, token, name, slug, cache)
        print(f"ensure tag {name} / {slug} -> {tid}")

    refresh_tag_cache(args.base, token, cache)
    articles = load_slug_map(args.base, token)
    print(f"articles in db: {len(articles)}, manifest: {len(items)}")

    ok = 0
    skip = 0
    for idx, item in enumerate(items, 1):
        slug = item["slug"]
        meta = articles.get(slug)
        if not meta:
            print(f"[{idx}/{len(items)}] skip missing {slug}")
            skip += 1
            continue
        pairs = tags_for_category(item.get("categorySlug") or "")
        tag_ids = [ensure_tag(args.base, token, n, s, cache) for n, s in pairs]
        try:
            aid = retag_one(args.base, token, item, meta, tag_ids, args.status)
            ok += 1
            print(f"[{idx}/{len(items)}] ok id={aid} {slug} tags={[p[0] for p in pairs]}")
        except Exception as e:
            print(f"[{idx}/{len(items)}] FAIL {slug}: {e}")
        time.sleep(args.sleep)

    keep = set(needed.keys()) | KEEP_EXTRA_SLUGS
    # 保留仍有关联的演示标签；清理其余
    if not args.no_prune:
        print("pruning unused / messy tags...")
        # 先刷新：只删不在 keep 里的
        deleted = prune_tags(args.base, token, keep)
        print(f"pruned {deleted} tags")

    print(f"done: retagged={ok} skipped={skip}")


if __name__ == "__main__":
    main()
