#!/bin/sh
# Registers Debezium Postgres outbox connectors (odds + betting).
# Retries until Kafka Connect REST API is available.

CONNECT_URL="http://debezium-connect:8083"

echo "Waiting for Kafka Connect to be ready..."
until curl -sf "$CONNECT_URL/connectors" > /dev/null 2>&1; do
  echo "  Kafka Connect not ready yet - retrying in 5s..."
  sleep 5
done

register_if_missing() {
  CONNECTOR_NAME="$1"
  CONNECTOR_FILE="$2"
  echo "Checking connector '${CONNECTOR_NAME}'..."
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/connectors/${CONNECTOR_NAME}")
  if [ "$STATUS" = "200" ]; then
    echo "  Connector '${CONNECTOR_NAME}' already registered - skipping."
  else
    echo "  Registering '${CONNECTOR_NAME}'..."
    RESULT=$(curl -s -X POST "$CONNECT_URL/connectors" \
      -H "Content-Type: application/json" \
      -d @"$CONNECTOR_FILE")
    echo "  Response: $RESULT"
  fi
}

register_if_missing "odds-outbox-connector" "/odds-outbox-connector.json"
register_if_missing "betting-outbox-connector" "/betting-outbox-connector.json"

echo "Connector registration pass complete."
