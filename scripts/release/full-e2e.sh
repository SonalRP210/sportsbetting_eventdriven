#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8070/api/v1}"

echo "Checking gateway health..."
curl -fsS "${BASE_URL}/health" >/dev/null

echo "Upserting user profile..."
curl -fsS -X POST "${BASE_URL}/users" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","email":"user1@example.com","status":"ACTIVE"}' >/dev/null

echo "Submitting provider event..."
curl -fsS -X POST "${BASE_URL}/providers/events" \
  -H "Content-Type: application/json" \
  -d '{"provider":"demo","eventType":"SPORT_EVENT","payload":{"eventId":"event-001","home":"HOME","away":"AWAY"}}' >/dev/null

echo "Fetching access token from Keycloak (Docker: http://localhost:8090 by default)..."
KEYCLOAK_TOKEN_URL="${KEYCLOAK_TOKEN_URL:-http://localhost:8090/realms/sportsbetting/protocol/openid-connect/token}"
ACCESS_TOKEN="$(curl -sS -X POST "${KEYCLOAK_TOKEN_URL}" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=odds-gateway&username=feeder&password=feeder' \
  | python -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')"

echo "Sending odds feed..."
curl -fsS -X POST "${BASE_URL}/odds-feed" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '[{"eventId":"event-001","selection":"HOME","odds":2.10}]' >/dev/null

echo "Waiting for odds projection to betting-service..."
python - <<PY "${ACCESS_TOKEN}"
import json
import sys
import time
import urllib.error
import urllib.request

token = sys.argv[1]
url = "http://localhost:8070/api/v1/events/event-001/bets?page=0&size=1"
req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token})
deadline = time.time() + 30
last_error = None
while time.time() < deadline:
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            if r.status < 500:
                raise SystemExit(0)
    except Exception as exc:
        last_error = exc
    time.sleep(1)
raise SystemExit(f"projection did not settle in time: {last_error}")
PY

echo "Placing bet..."
BET_RESPONSE="$(curl -fsS -X POST "${BASE_URL}/bets" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e2e-bet-1" \
  -d '{"userId":"user-1","eventId":"event-001","selection":"HOME","stake":10.00}')"

BET_ID="$(python - <<'PY' "$BET_RESPONSE"
import json
import sys
obj = json.loads(sys.argv[1])
print(obj.get("betId", ""))
PY
)"

if [[ -z "${BET_ID}" ]]; then
  echo "Failed to extract betId from response: ${BET_RESPONSE}" >&2
  exit 1
fi

echo "Fetching placed bet ${BET_ID}..."
curl -fsS "${BASE_URL}/bets/${BET_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" >/dev/null

echo "Settling event..."
curl -fsS -X POST "${BASE_URL}/events/settlements" \
  -H "Content-Type: application/json" \
  -d '{"eventId":"event-001","winningSelection":"HOME"}' >/dev/null

echo "Checking read models..."
curl -fsS "${BASE_URL}/users/user-1/exposure" >/dev/null
curl -fsS "${BASE_URL}/wallet/user-1/balance" >/dev/null
curl -fsS "${BASE_URL}/risk/total" >/dev/null

echo "Verifying betting-service projected bet outcome (WON) after settlement..."
python - <<PY "${BASE_URL}" "${BET_ID}" "${ACCESS_TOKEN}"
import json
import sys
import urllib.request

base = sys.argv[1].rstrip("/")
bet_id = sys.argv[2]
token = sys.argv[3]
url = f"{base}/bets/{bet_id}"
req = urllib.request.Request(url, headers={"Authorization": "Bearer " + token})
with urllib.request.urlopen(req, timeout=10) as r:
    body = json.load(r)
status = body.get("status", "")
if status != "WON":
    raise SystemExit(f"expected bet status WON after settlement on HOME, got {status!r}")
PY

echo "Full E2E release gate passed."
