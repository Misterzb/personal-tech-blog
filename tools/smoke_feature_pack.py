#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Feature pack smoke test against running server http://localhost:8080

Requires captcha disabled for auth steps, e.g.:
  BLOG_CAPTCHA_ENABLED=false
or blog.security.captcha-enabled=false in application-local.yml
"""

from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = "http://localhost:8080"


def req(method: str, path: str, body: dict | None = None, token: str | None = None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="ignore")
        try:
            return e.code, json.loads(raw) if raw else {"message": raw}
        except json.JSONDecodeError:
            return e.code, {"message": raw}


def ok(name: str, cond: bool, detail: str = ""):
    status = "PASS" if cond else "FAIL"
    print(f"[{status}] {name}" + (f" — {detail}" if detail else ""))
    return cond


def main() -> int:
    fails = 0
    phone = "1" + str(int(time.time()) % 10_000_000_000).zfill(10)
    if len(phone) != 11:
        phone = "139" + str(uuid.uuid4().int % 10**8).zfill(8)

    code, data = req("GET", "/actuator/health")
    if not ok("actuator health", code == 200 and (data.get("status") == "UP" or "status" in data), str(data)[:120]):
        fails += 1

    code, data = req("GET", "/api/public/captcha")
    captcha_ok = code == 200 and data.get("code") == 0 and (data.get("data") or {}).get("captchaId")
    if not ok("captcha endpoint", bool(captcha_ok), str(data)[:120]):
        fails += 1

    code, data = req("POST", "/api/auth/register", {
        "phone": phone,
        "password": "smoke1234",
        "nickname": "smoke",
        "email": "smoke@example.com",
    })
    token = (data.get("data") or {}).get("token")
    if not ok("register", code == 200 and data.get("code") == 0 and token, str(data)[:120]):
        msg = str(data.get("message") or data)
        if "验证码" in msg:
            print("HINT: set BLOG_CAPTCHA_ENABLED=false (or blog.security.captcha-enabled=false) for smoke auth")
        fails += 1
        return fails

    code, data = req("POST", "/api/auth/login", {"phone": phone, "password": "smoke1234"})
    if not ok("login", code == 200 and data.get("code") == 0):
        fails += 1

    code, data = req("PUT", "/api/auth/profile",
                     {"nickname": "smoke2", "email": "s2@example.com"}, token)
    if not ok("update profile", code == 200 and data.get("code") == 0):
        fails += 1

    code, data = req("GET", "/api/public/articles?page=1&size=1")
    records = ((data.get("data") or {}).get("records") or [])
    article_id = records[0]["id"] if records else None
    category_id = records[0].get("categoryId") if records else None
    if not ok("list articles", article_id is not None, f"id={article_id}"):
        fails += 1

    if article_id:
        code, data = req("POST", f"/api/auth/favorites/{article_id}", token=token)
        if not ok("favorite", code == 200 and data.get("code") == 0):
            fails += 1

    if article_id and category_id:
        code, data = req("POST", "/api/auth/reading-progress",
                         {"categoryId": category_id, "articleId": article_id}, token)
        if not ok("reading progress", code == 200 and data.get("code") == 0):
            fails += 1

    code, data = req("GET", "/api/public/search?q=Spring")
    if not ok("fulltext search", code == 200 and data.get("code") == 0):
        fails += 1

    code, data = req("GET", "/api/public/announcements")
    if not ok("announcements", code == 200 and data.get("code") == 0 and isinstance(data.get("data"), list)):
        fails += 1

    code, data = req("GET", "/api/public/friend-links")
    if not ok("friend-links", code == 200 and data.get("code") == 0):
        fails += 1

    code, data = req("POST", "/api/admin/auth/login",
                     {"username": "admin", "password": "admin123"})
    if not ok("admin login", code == 200 and data.get("code") == 0 and (data.get("data") or {}).get("token")):
        msg = str(data.get("message") or data)
        if "验证码" in msg:
            print("HINT: set BLOG_CAPTCHA_ENABLED=false for admin login smoke")
        fails += 1

    print(f"\nDone. failures={fails}")
    return fails


if __name__ == "__main__":
    sys.exit(main())
