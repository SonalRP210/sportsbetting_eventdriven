#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-infra/local/docker-compose.yml}"
BASE_URL="${BASE_URL:-http://localhost:8070/api/v1}"

echo "Initial health check..."
curl -fsS "${BASE_URL}/health" >/dev/null

echo "Restarting broker (redpanda) to simulate transient outage..."
docker compose -f "${COMPOSE_FILE}" restart redpanda

echo "Waiting for broker to become healthy again..."
python - <<'PY'
import subprocess
import time

deadline = time.time() + 120
while time.time() < deadline:
    result = subprocess.run(
        ["docker", "compose", "-f", "infra/local/docker-compose.yml", "ps", "redpanda", "--format", "json"],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode == 0 and "healthy" in result.stdout.lower():
        raise SystemExit(0)
    time.sleep(2)
raise SystemExit("redpanda did not become healthy after restart")
PY

echo "Verifying system remains functional post-restart..."
curl -fsS "${BASE_URL}/health" >/dev/null
curl -fsS "${BASE_URL}/gateway/routes" >/dev/null
curl -fsS -X POST "${BASE_URL}/odds-feed" \
  -H "Content-Type: application/json" \
  -d '[{"eventId":"event-res","selection":"HOME","odds":1.95}]' >/dev/null

echo "Resilience scenario passed."
