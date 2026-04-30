# Migration Runbook

## Goal

Replicate existing sports betting behavior from the current monolith into event-driven microservices without changing business outcomes.

## Source Mapping

- Controllers in `src/main/java/com/sonal/sportsbetting/controller` map to gateway-facing APIs.
- `DefaultBetPlacementService`, `DefaultBetQueryService`, `DefaultBettingService` -> `services/betting-service`
- `DefaultOddsService` and publisher/consumer classes -> `services/odds-service`
- `DefaultSettlementService` and settlement ledger model -> `services/settlement-service`
- `DefaultExposureService` and projections -> `services/risk-service`

## Sequencing

1. Keep monolith APIs as reference contract from `shared/contracts/openapi.yaml`.
2. Implement producer/consumer contracts from `shared/contracts/events`.
3. Move read paths first, then write paths with idempotency parity.
4. Validate behavior parity using existing tests as baseline.
