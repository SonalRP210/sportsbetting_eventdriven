/**
 * Load test: POST /api/v1/odds-feed via api-gateway (Keycloak password grant in setup).
 *
 * api-gateway enforces ~300 req/min per (client IP + URI). Default scenario uses
 * constant-arrival-rate at 4.5 iter/s to stay under that while still moving metrics.
 *
 * Grafana (load-test stack): http://localhost:3000 (admin/admin)
 *   SportsBetting - Load Test - Event Platform
 *   http://localhost:3000/d/sportsbetting-loadtest-event-platform/sportsbetting-load-test-event-platform
 *   Variable "Microservice": odds-service or api-gateway.
 *
 * Env: GATEWAY_BASE_URL, KEYCLOAK_TOKEN_URL, OIDC_*, ACCESS_TOKEN,
 *      ODDS_GATEWAY_ARRIVAL_RATE (default 4 int iter/s), ODDS_GATEWAY_DURATION (default 5m),
 *      ODDS_GATEWAY_PREALLOC_VUS, ODDS_GATEWAY_MAX_VUS
 * Do not set K6_DURATION / K6_VUS in the shell: k6 v2 applies those as global overrides and removes this scenario.
 *
 * Example ~10k iter/s (needs enough VUs: roughly rate * p95_latency_seconds; try 800/3000):
 *   export ODDS_GATEWAY_ARRIVAL_RATE=10000
 *   export ODDS_GATEWAY_DURATION=60s
 *   export ODDS_GATEWAY_PREALLOC_VUS=800
 *   export ODDS_GATEWAY_MAX_VUS=3000
 *   k6 run scripts/performance/k6-odds-feed-gateway.js
 * (Load-test compose disables gateway IP rate limit via GATEWAY_RATE_LIMIT_REQUESTS_PER_MINUTE=0.)
 */
import http from "k6/http";
import { check } from "k6";

const arrival = parseInt(__ENV.ODDS_GATEWAY_ARRIVAL_RATE || "4", 10);
const duration = __ENV.ODDS_GATEWAY_DURATION || "5m";
const pre = parseInt(__ENV.ODDS_GATEWAY_PREALLOC_VUS || "15", 10);
const maxv = parseInt(__ENV.ODDS_GATEWAY_MAX_VUS || "50", 10);

export const options = {
  scenarios: {
    odds_feed: {
      executor: "constant-arrival-rate",
      rate: isNaN(arrival) || arrival < 1 ? 4 : arrival,
      timeUnit: "1s",
      duration: duration,
      preAllocatedVUs: isNaN(pre) || pre < 1 ? 15 : pre,
      maxVUs: isNaN(maxv) || maxv < pre ? 50 : maxv,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.15"],
    http_req_duration: ["p(95)<5000"],
  },
};

const gatewayRoot = __ENV.GATEWAY_BASE_URL || "http://localhost:8070";
const tokenUrl =
  __ENV.KEYCLOAK_TOKEN_URL ||
  "http://localhost:8090/realms/sportsbetting/protocol/openid-connect/token";

export function setup() {
  const tokenBody =
    __ENV.ACCESS_TOKEN ||
    (() => {
      const form = [
        "grant_type=password",
        "client_id=" + (__ENV.OIDC_CLIENT_ID || "odds-gateway"),
        "username=" + (__ENV.OIDC_USERNAME || "feeder"),
        "password=" + (__ENV.OIDC_PASSWORD || "feeder"),
      ].join("&");
      const tr = http.post(tokenUrl, form, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });
      if (tr.status !== 200) {
        throw new Error(
          "token request failed: " + tr.status + " " + String(tr.body).slice(0, 200)
        );
      }
      const j = tr.json();
      return j.access_token;
    })();

  return { token: tokenBody };
}

export default function (data) {
  const id = `${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify([
    {
      eventId: "event-load-" + id,
      selection: "HOME",
      odds: 1.95 + (__ITER % 10) * 0.01,
    },
  ]);

  const res = http.post(`${gatewayRoot}/api/v1/odds-feed`, payload, {
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + data.token,
    },
    tags: { endpoint: "odds-feed" },
  });

  check(res, {
    "2xx accepted": (r) => r.status >= 200 && r.status < 300,
  });
}
