"""把仍带 -N 后缀的 lxf 文章改回规范 slug（先挤掉软删除占用）。"""
from __future__ import annotations

import json
import re
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080"


def http_json(method, url, data=None, token=None):
    body = None
    headers = {"Content-Type": "application/json; charset=utf-8", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    tok = http_json(
        "POST",
        f"{BASE}/api/admin/auth/login",
        {"username": "admin", "password": "admin123"},
    )["data"]["token"]

    arts = []
    page = 1
    while page <= 50:
        d = http_json("GET", f"{BASE}/api/admin/articles?page={page}&size=100", token=tok)["data"]
        arts.extend(d.get("records") or [])
        if page * 100 >= (d.get("total") or 0):
            break
        page += 1

    lxf = [a for a in arts if (a.get("slug") or "").startswith("lxf-")]
    by_slug = {a["slug"]: a for a in lxf}

    for a in lxf:
        slug = a["slug"]
        m = re.match(r"^(.+)-(\d+)$", slug)
        if not m:
            continue
        canon = m.group(1)
        if canon in by_slug:
            print(f"skip {slug}: canon still live as {canon}")
            continue
        detail = http_json("GET", f"{BASE}/api/admin/articles/{a['id']}", token=tok)["data"]
        tag_ids = [t["id"] for t in (detail.get("tags") or [])]
        payload = {
            "id": detail["id"],
            "title": detail["title"],
            "slug": canon,
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
        try:
            http_json("POST", f"{BASE}/api/admin/articles", payload, token=tok)
            print(f"ok {slug} -> {canon}")
            by_slug[canon] = a
            by_slug.pop(slug, None)
        except Exception as e:
            print(f"FAIL {slug}: {e}")
        time.sleep(0.05)
    print("done")


if __name__ == "__main__":
    main()
