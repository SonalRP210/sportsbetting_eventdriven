#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-infra/local/docker-compose.yml}"
BASE_URL="${BASE_URL:-http://localhost:8070/api/v1}"

echo "Pre-check: gateway health"
curl -fsS "${BASE_URL}/health" >/dev/null

echo "Restarting broker..."
docker compose -f "${COMPOSE_FILE}" restart redpanda

echo "Waiting for broker health..."
for i in {1..60}; do
  if docker compose -f "${COMPOSE_FILE}" ps | grep -q "sb-redpanda.*healthy"; then
    break
  fi
  sleep 2
done

echo "Post-check: service still responds"
curl -fsS "${BASE_URL}/health" >/dev/null
echo "Chaos broker restart scenario passed."
