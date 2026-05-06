import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 10,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1000"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:18089";

export default function () {
  const res = http.get(`${baseUrl}/api/v1/risk/total`);
  check(res, {
    "risk endpoint returns 2xx/4xx": (r) => (r.status >= 200 && r.status < 500),
  });
  sleep(0.1);
}
