# Event Catalog

Derived from upstream domain events and aligned for service ownership.

## Topics

- `betting.bet.placed.v1`
- `betting.bet.cancelled.v1`
- `odds.odds.updated.v1`
- `settlement.event.settled.v1`

## Publishers

- `services/betting-service` -> bet placed/cancelled
- `services/odds-service` -> odds updated
- `services/settlement-service` -> event settled

## Primary Consumers

- `services/risk-service` consumes bet and settlement events
- `services/wallet-service` consumes settlement events
- `services/notification-service` consumes all customer-facing lifecycle events
