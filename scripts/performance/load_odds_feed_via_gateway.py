#!/usr/bin/env python3
"""
Sustained load on POST /api/v1/odds-feed via api-gateway (stdlib only; no k6 required).

api-gateway RateLimitingFilter: 300 requests/min per (client IP + URI). Use --max-rps
(default 4.5) to pace all workers globally and avoid HTTP 429. Set --max-rps 0 to stress
the limiter on purpose.

Grafana (load-test stack): http://localhost:3000 (admin/admin)
  Dashboard: SportsBetting - Load Test - Event Platform
  http://localhost:3000/d/sportsbetting-loadtest-event-platform/sportsbetting-load-test-event-platform
  Template variable "Microservice": odds-service (or api-gateway for edge).

Examples:
  python scripts/performance/load_odds_feed_via_gateway.py -d 300 -w 20
  python scripts/performance/load_odds_feed_via_gateway.py -d 60 --max-rps 0 -w 50
"""
from __future__ import annotations

import argparse
import json
import random
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Any, Optional


@dataclass
class Stats:
    lock: threading.Lock = field(default_factory=threading.Lock)
    ok: int = 0
    fail: int = 0
    last_err: str = ""

    def add_ok(self) -> None:
        with self.lock:
            self.ok += 1

    def add_fail(self, msg: str) -> None:
        with self.lock:
            self.fail += 1
            self.last_err = msg[:200]


class GlobalRpsPacer:
    """At most one caller proceeds per (1/max_rps) seconds (process-wide)."""

    def __init__(self, max_rps: float):
        self.min_interval = 1.0 / max_rps if max_rps > 0 else 0.0
        self.lock = threading.Lock()
        self.next_at = 0.0

    def wait(self) -> None:
        if self.min_interval <= 0:
            return
        with self.lock:
            now = time.monotonic()
            if now < self.next_at:
                time.sleep(self.next_at - now)
                now = time.monotonic()
            self.next_at = now + self.min_interval


def http_post(url: str, headers: dict[str, str], body: bytes, timeout: float) -> tuple[int, bytes]:
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return int(resp.status), resp.read()


def fetch_token(token_url: str, client_id: str, username: str, password: str, timeout: float) -> str:
    form = (
        f"grant_type=password&client_id={urllib.parse.quote(client_id, safe='')}"
        f"&username={urllib.parse.quote(username, safe='')}"
        f"&password={urllib.parse.quote(password, safe='')}"
    ).encode()
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    status, data = http_post(token_url, headers, form, timeout)
    if status != 200:
        raise SystemExit(f"token HTTP {status}: {data[:500]!r}")
    j: dict[str, Any] = json.loads(data.decode())
    tok = j.get("access_token")
    if not tok:
        raise SystemExit(f"no access_token in response: {j}")
    return str(tok)


def worker(
    base: str,
    token: str,
    stop_at: float,
    sleep_s: float,
    stats: Stats,
    timeout: float,
    pacer: Optional[GlobalRpsPacer],
) -> None:
    feed_url = f"{base.rstrip('/')}/api/v1/odds-feed"
    hdrs = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
    }
    while time.monotonic() < stop_at:
        if pacer is not None:
            pacer.wait()
        eid = f"evt-py-{threading.get_ident()}-{random.randint(0, 10**9)}"
        body = json.dumps(
            [{"eventId": eid, "selection": "HOME", "odds": round(1.9 + random.random() * 0.2, 4)}]
        ).encode()
        try:
            status, _ = http_post(feed_url, hdrs, body, timeout)
            if 200 <= status < 300:
                stats.add_ok()
            else:
                stats.add_fail(f"HTTP {status}")
        except urllib.error.HTTPError as e:
            try:
                body_snip = e.read()[:120]
            except Exception:  # noqa: BLE001
                body_snip = b""
            stats.add_fail(f"HTTPError {e.code} {body_snip!r}")
        except Exception as e:  # noqa: BLE001
            stats.add_fail(repr(e))
        if sleep_s > 0:
            time.sleep(sleep_s)


def main() -> int:
    p = argparse.ArgumentParser(description="Load-test odds-feed via api-gateway")
    p.add_argument("--gateway", default="http://localhost:8070", help="api-gateway root URL")
    p.add_argument(
        "--token-url",
        default="http://localhost:8090/realms/sportsbetting/protocol/openid-connect/token",
    )
    p.add_argument("--client-id", default="odds-gateway")
    p.add_argument("--username", default="feeder")
    p.add_argument("--password", default="feeder")
    p.add_argument("-d", "--duration", type=int, default=300, help="seconds")
    p.add_argument("-w", "--workers", type=int, default=20, help="concurrent worker threads")
    p.add_argument("--sleep", type=float, default=0.0, help="extra seconds after each request per worker")
    p.add_argument(
        "--max-rps",
        type=float,
        default=4.5,
        help="global max requests/s (0 = no pacing; gateway allows ~5/s before 429)",
    )
    p.add_argument("--timeout", type=float, default=30.0, help="per-request timeout seconds")
    args = p.parse_args()

    token = fetch_token(
        args.token_url, args.client_id, args.username, args.password, args.timeout
    )
    pacer = None if args.max_rps <= 0 else GlobalRpsPacer(args.max_rps)
    pace_note = f"max {args.max_rps} req/s globally" if pacer else "no global pacing (may hit 429)"
    print(f"Token OK. {args.workers} workers, {args.duration}s, {pace_note} -> {args.gateway}/api/v1/odds-feed")
    print(
        "Grafana: http://localhost:3000/d/sportsbetting-loadtest-event-platform/"
        "sportsbetting-load-test-event-platform (Microservice: odds-service)"
    )

    stats = Stats()
    stop_at = time.monotonic() + args.duration

    def reporter() -> None:
        while time.monotonic() < stop_at + 0.5:
            time.sleep(5)
            with stats.lock:
                print(f"  ... {stats.ok} ok, {stats.fail} fail  (last error: {stats.last_err or 'none'})")

    threading.Thread(target=reporter, daemon=True).start()

    threads: list[threading.Thread] = []
    for _ in range(max(1, args.workers)):
        t = threading.Thread(
            target=worker,
            args=(args.gateway, token, stop_at, args.sleep, stats, args.timeout, pacer),
            daemon=True,
        )
        t.start()
        threads.append(t)

    for t in threads:
        t.join()

    with stats.lock:
        print(f"Done. {stats.ok} ok, {stats.fail} fail")
    return 0 if stats.fail == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
