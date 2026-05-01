import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 10,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8070/api/v1";

export default function () {
  const id = `${__VU}-${__ITER}`;
  const res = http.post(
    `${baseUrl}/bets`,
    JSON.stringify({
      userId: `perf-user-${id}`,
      eventId: "event-001",
      selection: "HOME",
      stake: 10.0,
    }),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": `perf-idem-${id}`,
      },
    }
  );

  check(res, {
    "bet placed or validation rejected": (r) => [201, 400, 422].includes(r.status),
  });
  sleep(0.1);
}
