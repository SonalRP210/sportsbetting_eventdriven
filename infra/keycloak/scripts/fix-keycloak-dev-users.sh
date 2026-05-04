#!/bin/sh
# Clears required user actions, sets emailVerified, and ensures email + first/last name
# so grant_type=password works on KC 24+ (otherwise misleading "Account is not fully set up").
# Run once after Keycloak is healthy (docker compose one-shot service).
# No backslash line continuations (Windows CRLF bind mounts break those in ash).

set -eu

apk add --no-cache curl jq >/dev/null

KEYCLOAK_URL="${KEYCLOAK_INTERNAL_URL:-http://keycloak:8080}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="${KEYCLOAK_REALM:-sportsbetting}"

echo "Waiting for realm ${REALM} at ${KEYCLOAK_URL}..." >&2
i=0
while [ "$i" -lt 90 ]; do
  if curl -sf "${KEYCLOAK_URL}/realms/${REALM}/.well-known/openid-configuration" >/dev/null; then
    break
  fi
  i=$((i + 1))
  sleep 2
done
if [ "$i" -ge 90 ]; then
  echo "timeout: realm ${REALM} not reachable" >&2
  exit 1
fi

echo "Fetching admin token (master / admin-cli)..." >&2
TOKEN_JSON=$(curl -sS -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password&client_id=admin-cli&username=${ADMIN_USER}&password=${ADMIN_PASS}")

TOKEN=$(printf '%s' "$TOKEN_JSON" | jq -r '.access_token // empty')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "admin token failed: $(printf '%s' "$TOKEN_JSON" | jq -c . 2>/dev/null || printf '%s' "$TOKEN_JSON")" >&2
  exit 1
fi

fix_user() {
  USERNAME="$1"
  LIST=$(curl -sS -H "Authorization: Bearer ${TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}&exact=true")
  USER_ID=$(printf '%s' "$LIST" | jq -r '.[0].id // empty')
  if [ -z "$USER_ID" ] || [ "$USER_ID" = "null" ]; then
    echo "skip: no user ${USERNAME}" >&2
    return 0
  fi
  FULL=$(curl -sS -H "Authorization: Bearer ${TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}")
  BODY=$(printf '%s' "$FULL" | jq --arg un "$USERNAME" 'del(.userProfileMetadata) | .requiredActions = [] | .emailVerified = true | .email = (if (.email == null or .email == "") then ($un + "@local.dev") else .email end) | .firstName = (if (.firstName == null or .firstName == "") then $un else .firstName end) | .lastName = (if (.lastName == null or .lastName == "") then "User" else .lastName end)')
  CODE=$(curl -sS -o /tmp/kc_put_body.txt -w "%{http_code}" -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_ID}" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d "$BODY")
  if [ "$CODE" != "204" ] && [ "$CODE" != "200" ]; then
    echo "PUT ${USERNAME} failed HTTP ${CODE}: $(cat /tmp/kc_put_body.txt)" >&2
    exit 1
  fi
  echo "updated ${USERNAME} (profile + requiredActions for password grant)" >&2
}

for u in viewer feeder ops admin; do
  fix_user "$u"
done

echo "keycloak dev users OK" >&2
