# Event Catalog

Derived from upstream domain events and aligned for service ownership.

## Topics

- `betting.bet.placed.v1`
- `betting.bet.cancelled.v1`
- `odds.odds.updated.v1`
- `settlement.event.settled.v1`

## Publishers

- `services/betting-service` -> bet placed/cancelled (transactional outbox; **Debezium CDC** relay from `betting.outbox_events` in local Docker — same pattern as odds-service)
- `services/odds-service` -> odds updated
- `services/settlement-service` -> event settled

## Primary Consumers

- `services/betting-service` consumes `odds.updated.v1` (local quote cache) and `settlement.event.settled.v1` (updates bet read model to WON/LOST)
- `services/risk-service` consumes bet and settlement events
- `services/wallet-service` consumes settlement events
- `services/notification-service` consumes all customer-facing lifecycle events
