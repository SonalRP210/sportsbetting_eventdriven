#!/usr/bin/env bash

BROKER="redpanda:9092"

until rpk topic list --brokers "$BROKER" >/dev/null 2>&1; do
  echo "Waiting for Kafka broker..."
  sleep 2
done

TOPICS=(
  betting.bet.placed.v1 betting.bet.cancelled.v1 odds.updated.v1 settlement.event.settled.v1
  wallet.wallet.credited.v1 wallet.wallet.debited.v1 risk.exposure.updated.v1 notification.events.v1
  betting.bet.placed.v1.dlq betting.bet.cancelled.v1.dlq odds.updated.v1.dlq settlement.event.settled.v1.dlq
  wallet.wallet.credited.v1.dlq wallet.wallet.debited.v1.dlq risk.exposure.updated.v1.dlq notification.events.v1.dlq
)

for t in "${TOPICS[@]}"; do
  rpk topic create "$t" --brokers "$BROKER" --partitions 3 --replicas 1 || true
done

echo "Topics created/verified"
