"""
清理 lxf-* 重复文章：按「去掉末尾 -数字」的规范 slug 分组，每组只留 sortOrder 最大的一篇。
"""
from __future__ import annotations

import json
import re
import time
import urllib.error
import urllib.request
from collections import defaultdict

BASE = "http://localhost:8080"
USER = "admin"
PASSWORD = "admin123"


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
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> {e.code}: {err}") from e


def login() -> str:
    res = http_json("POST", f"{BASE}/api/admin/auth/login", {"username": USER, "password": PASSWORD})
    return res["data"]["token"]


def load_all(token: str) -> list[dict]:
    out = []
    page = 1
    while page <= 50:
        res = http_json("GET", f"{BASE}/api/admin/articles?page={page}&size=100", token=token)
        data = res.get("data") or {}
        out.extend(data.get("records") or [])
        if page * 100 >= (data.get("total") or 0):
            break
        page += 1
    return out


def get_detail(token: str, aid: int) -> dict | None:
    res = http_json("GET", f"{BASE}/api/admin/articles/{aid}", token=token)
    return res.get("data")


def save(token: str, detail: dict, **overrides):
    tag_ids = [t["id"] for t in (detail.get("tags") or [])]
    payload = {
        "id": detail["id"],
        "title": detail["title"],
        "slug": detail["slug"],
        "summary": detail.get("summary") or "",
        "contentMd": detail.get("contentMd") or "",
        "status": detail.get("status", 1),
        "categoryId": detail.get("categoryId"),
        "tagIds": tag_ids,
        "seoTitle": detail.get("seoTitle") or detail["title"],
        "seoDescription": detail.get("seoDescription") or "",
        "isTop": detail.get("isTop") or False,
        "sortOrder": detail.get("sortOrder") or 0,
    }
    payload.update(overrides)
    res = http_json("POST", f"{BASE}/api/admin/articles", payload, token=token)
    if res.get("code") != 0:
        raise RuntimeError(res.get("message"))
    return res["data"]


def delete(token: str, aid: int):
    http_json("DELETE", f"{BASE}/api/admin/articles/{aid}", token=token)


def canonical_slug(slug: str) -> str:
    s = slug
    # strip -trash-ID and -deleted-ID tails first
    s = re.sub(r"-(?:trash|deleted)-\d+$", "", s)
    while True:
        m = re.match(r"^(.+)-(\d+)$", s)
        if not m:
            break
        s = m.group(1)
    return s


def main():
    token = login()
    articles = [a for a in load_all(token) if (a.get("slug") or "").startswith("lxf-")]
    groups: dict[str, list[dict]] = defaultdict(list)
    for a in articles:
        groups[canonical_slug(a["slug"])].append(a)

    multi = {k: v for k, v in groups.items() if len(v) > 1}
    print(f"groups with dups: {len(multi)}")

    for canon, items in multi.items():
        items_sorted = sorted(
            items,
            key=lambda x: (int(x.get("sortOrder") or 0), -len(x["slug"]), x["id"]),
            reverse=True,
        )
        keep = items_sorted[0]
        drops = items_sorted[1:]
        print(
            f"canon={canon} keep id={keep['id']} slug={keep['slug']} sort={keep.get('sortOrder')} "
            f"drop={[d['id'] for d in drops]}"
        )
        for drop in drops:
            detail = get_detail(token, drop["id"])
            if not detail:
                print(f"  skip missing {drop['id']}")
                continue
            try:
                save(token, detail, slug=f"lxf-trash-{drop['id']}")
            except Exception as e:
                print(f"  trash rename fail {drop['id']}: {e}")
            time.sleep(0.03)
            try:
                delete(token, drop["id"])
            except Exception as e:
                print(f"  delete fail {drop['id']}: {e}")
            time.sleep(0.03)

        if keep["slug"] != canon:
            detail = get_detail(token, keep["id"])
            if detail:
                try:
                    save(
                        token,
                        detail,
                        slug=canon,
                        sortOrder=int(keep.get("sortOrder") or 0),
                    )
                    print(f"  renamed keep -> {canon}")
                except Exception as e:
                    print(f"  rename keep fail: {e}")
                time.sleep(0.03)

    print("cleanup done")


if __name__ == "__main__":
    main()
