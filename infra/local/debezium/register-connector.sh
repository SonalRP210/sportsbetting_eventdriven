#!/bin/sh
# Registers the odds outbox Debezium connector.
# Retries until Kafka Connect REST API is available.

CONNECT_URL="http://debezium-connect:8083"
CONNECTOR_FILE="/odds-outbox-connector.json"

echo "Waiting for Kafka Connect to be ready..."
until curl -sf "$CONNECT_URL/connectors" > /dev/null 2>&1; do
  echo "  Kafka Connect not ready yet — retrying in 5s..."
  sleep 5
done

echo "Kafka Connect is ready. Checking if connector already exists..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/connectors/odds-outbox-connector")

if [ "$STATUS" = "200" ]; then
  echo "Connector 'odds-outbox-connector' already registered — skipping."
else
  echo "Registering connector..."
  RESULT=$(curl -s -X POST "$CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d @"$CONNECTOR_FILE")
  echo "Response: $RESULT"
  echo "Connector registered."
fi
