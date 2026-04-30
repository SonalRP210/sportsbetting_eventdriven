# Architecture Evolution Log

This is the living architecture document for the Sports Betting service.

It has two purposes:

1. Capture the current architecture as-is.
2. Record each scalability evolution step in the same file as we implement it.

**How to read this file:** the **baseline** section below is the **full pre–Phase 1** record (kept for history). **Phase 1** is documented as **additions and deltas** on top of that baseline; it does not delete the baseline narrative.

We will update this document incrementally after each change so architecture and code stay aligned.

---

## Diagram (current — after Phase 1)

Sports Betting — Phase 1 architecture

*Figure: clients hit a load balancer; multiple Spring Boot instances share **Redis** (global rate-limit counters + odds pub/sub) and **PostgreSQL** (durable domain data + exposure aggregate). Each instance keeps a **local odds cache** for fast reads.*

---

## Baseline architecture (pre–Phase 1, full record — not removed)

This section is the original “current as-built” description **before** Redis, pub/sub odds fan-out, and DB-backed total exposure. It remains here unchanged in spirit so you can diff mentally against Phase 1.

### Runtime model

- Spring Boot (Spring MVC, blocking request/response).
- Request-level concurrency is provided by the servlet container thread pool.
- Business services are synchronous and transaction-based (`@Transactional`).
- No explicit async pipelines (`@Async`, `CompletableFuture`, schedulers, reactive streams) in the baseline implementation.

### Core components and responsibilities

- `DefaultOddsService`
  - Stores latest odds in an in-memory `ConcurrentHashMap`.
  - Reads are local to the current JVM instance.
- `DefaultBetPlacementService`
  - Places bets with optional idempotency (`Idempotency-Key`).
  - Uses DB unique constraint (`user_id`, `idempotency_key`) to prevent duplicate logical bets.
  - Uses retry template for transient persistence errors.
- `DefaultSettlementService`
  - Settles open bets for an event.
  - Uses DB row locking (`PESSIMISTIC_WRITE`) on open event bets during settlement.
  - Writes settlement ledger (`EventSettlement`) for idempotent replay and conflict detection.
- `DefaultExposureService`
  - Maintains total exposure as in-memory `AtomicReference<BigDecimal>`.
  - Provides user exposure by querying open bets and aggregating risk.
- `RateLimitingFilter`
  - Uses in-memory `ConcurrentHashMap` + atomic counters for per-client window tracking.

### Data consistency and concurrency controls

- **Idempotency for bet placement**
  - Implemented via DB uniqueness + replay/recovery on race.
- **Settlement idempotency**
  - Implemented via `EventSettlement` ledger keyed by `eventId`.
  - Same winner => replay; different winner => conflict (`409`).
- **Pessimistic locking**
  - Settlement path uses `@Lock(PESSIMISTIC_WRITE)` to serialize conflicting updates for open bets.
- **Optimistic locking**
  - `Bet` entity uses `@Version` field (`opt_lock`) for concurrent update detection.
- **Transient retry**
  - `RetryTemplate` retries transient persistence exceptions with fixed backoff.

### Baseline scaling characteristics

- Strong correctness for core write flows (bet idempotency, settlement serialization) because safety is DB-backed.
- In-memory components are node-local and therefore not globally consistent in multi-instance deployments:
  - odds cache
  - rate-limit counters
  - exposure accumulator

### Baseline known architectural limits

- Global rate limiting is not enforced across instances.
- Odds can diverge between instances.
- Total exposure is not durable and not globally authoritative.

---

## Phase 1 — additions and deltas (implemented on top of baseline)

Phase 1 **does not remove** the baseline behaviors above for DB-backed bets/settlement; it **extends** cross-node coordination and one read path as follows.

### Runtime model (unchanged from baseline)

- Still Spring MVC, synchronous services, servlet thread pool concurrency.

### Core components — what changed or was added

- `RateLimitingFilter` **+** `RateLimiterGateway`
  - Filter contract unchanged; counting delegated to a gateway.
  - `RedisRateLimiterGateway` (when `app.redis.enabled=true`): Lua `INCR` + `EXPIRE` on a shared Redis key per client+URI — **global** rate limit across instances.
  - `InMemoryRateLimiterGateway` (when Redis disabled): per-JVM sliding window — same class of behavior as baseline filter; **not** global under multi-instance.
- `DefaultOddsService`
  - Still keeps latest odds in a per-instance `ConcurrentHashMap`.
  - **Added:** on feed ingest, after local update, `OddsUpdateBroadcaster` publishes to Redis pub/sub (`app.redis.odds-channel`) when Redis is enabled so peer instances converge.
  - `NoOpOddsUpdateBroadcaster`**:** when Redis is off, no cross-node fan-out; **DEBUG** log explains instance-local only behavior.
- `DefaultBetPlacementService` **/** `DefaultSettlementService`
  - Unchanged in role from baseline (still DB-centric idempotency and locking).
- `DefaultExposureService`
  - **Changed read path:** `getTotalExposure()` uses PostgreSQL aggregate `sum(stake * odds)` for `OPEN` bets via `BetRepository.sumExposureByStatus`, normalizes, refreshes the in-process gauge.
  - **Still present:** `increaseExposure` / `decreaseExposure` update local `AtomicReference` between DB-backed reads (gauge behavior).

### Configuration added (`app.redis`)


| Property                          | Role                                                                        |
| --------------------------------- | --------------------------------------------------------------------------- |
| `app.redis.enabled`               | Toggle Redis-backed rate limit + odds pub/sub vs in-memory / no-op fan-out. |
| `app.redis.rate-limit-key-prefix` | Namespace for Redis rate-limit keys (e.g. `rate_limit:`).                   |
| `app.redis.odds-channel`          | Redis pub/sub channel for odds update messages.                             |


Connection: `spring.data.redis.host` / `port` (see `application-local.yaml`, `application-cloud.yaml`).

### Data consistency (baseline items still apply)

All baseline bullets under *Data consistency and concurrency controls* remain true. Phase 1 adds **cross-node** rate limit and odds **fan-out**, and a **DB-backed** total exposure **read**.

### Phase 1 checklist (coverage)


| Goal                         | Status | Notes                                                        |
| ---------------------------- | ------ | ------------------------------------------------------------ |
| Global rate limiting         | Done   | Redis Lua counter + TTL; in-memory fallback when Redis off.  |
| Cross-node odds convergence  | Done   | Pub/sub + local map; not yet a durable canonical odds store. |
| Authoritative total exposure | Done   | DB aggregate for `OPEN` on `getTotalExposure()`.             |


### Remaining limits after Phase 1

- Odds “latest” is still primarily **in-memory per instance**; pub/sub alone does not give a cold joiner full history without a **durable odds store** (Redis Hash / DB) + warm load.
- Redis memory, eviction, and HA are operational concerns.
- If callers read only the in-memory gauge without `getTotalExposure()`, exposure can diverge briefly from DB truth.

---

## Target scalable architecture (north star)

- Durable canonical odds (Redis Hash or DB) + pub/sub or log for notifications.
- Optional event-driven backbone (outbox + domain events).
- Exposure projection table if event-sourced accounting is required.

## Evolution plan (phased)

### Phase 1 (minimal-change hardening) — **implemented**

1. Externalize rate limiting to Redis (with in-memory fallback).
2. Authoritative total exposure from DB aggregate on read.
3. Synchronize odds across instances via Redis pub/sub + local cache.

### Phase 2 (consistency + boundaries) — **implemented**

1. Domain events (`BetPlaced`, `EventSettled`, `OddsUpdated`) with outbox pattern.
2. Exposure projection as read model.
3. Explicit write/read model boundaries.

#### Phase 2 implementation notes

- **Outbox + domain events**
  - Added `DomainEventOutbox` and dispatcher path (`DomainEventPublisher`, `OutboxDispatcher`, `OutboxPoller`) so business writes and event records are committed together.
  - Event payloads introduced for `BET_PLACED`, `BET_CANCELLED`, `EVENT_SETTLED`, `ODDS_UPDATED`.
  - Poller retries unprocessed events (`app.outbox.poll-interval-ms`) to avoid event loss on transient failures.
- **Exposure projection**
  - Added `exposure_projection` table and updater (`ExposureProjectionUpdater`) to maintain global and per-user open risk from events.
  - `DefaultExposureService` now reads projection rows instead of mutating in-process exposure state.
  - Startup initializer (`ExposureProjectionStartupInitializer`) backfills projection from existing open bets if projection is empty.
- **Odds write/read boundaries**
  - Added durable canonical `latest_odds` table (`LatestOdds`, `LatestOddsRepository`).
  - `DefaultOddsService` persists latest odds and emits `ODDS_UPDATED`; local cache is read-through/warm cache, not source of truth.
  - Optional Redis hash mirror (`RedisLatestOddsHashWriter`) keeps a shared latest snapshot when Redis is enabled.
- **Service boundary changes**
  - Placement/cancel/settlement flows now publish events; direct exposure increment/decrement APIs were removed from `ExposureService`.
  - Query paths read from projection/canonical stores.

### Phase 3 (throughput + operations) — **implemented**

1. Lock/settlement contention observability and tuning.
2. Backpressure/resilience for feed bursts.
3. Retries/timeouts under load and failure scenarios.

#### Phase 3 implementation notes

- **Settlement contention strategy**
  - Kept pessimistic locking and added lock timeout hint on settlement lock query (`jakarta.persistence.lock.timeout`).
  - Added settlement lock retry policy (`SettlementRetryConfiguration`) with focused retry classes for lock contention.
  - Added metrics for contention and throughput:
    - `events.settlement.lock.wait` (timer)
    - `events.settlement.lock.failures` (counter)
    - `events.settled.duration` (timer)
    - `events.settled.conflicts` (counter)
- **Idempotency observability**
  - Preserved replay counters and added explicit constraint violation signal:
    - `bets.placed.idempotent_replay`
    - `bets.placed.idempotent_replay_after_race`
    - `bets.placed.idempotency.constraint_violations`
- **Backpressure path for odds bursts**
  - Added Kafka support as optional buffering path for odds ingestion:
    - `OddsFeedPublisher` abstraction
    - `DirectOddsFeedPublisher` (default direct path)
    - `KafkaOddsFeedPublisher` + `KafkaOddsFeedConsumer` (buffered path)
  - `DefaultBettingService.consumeOddsFeed(...)` now delegates through publisher abstraction.
- **Class-level refactors**
  - Rate-limiter boundary simplified to `RateLimiterGateway.tryConsume(clientKey)` with policy owned by gateway implementations.
  - `DefaultOddsService` split via collaborators:
    - `OddsPublisher` (`DefaultOddsPublisher`) for outbound fan-out
    - `OddsCacheUpdater` for inbound synchronization and cache updates
  - `DefaultExposureService` remains projection-backed with gauge as derived telemetry cache.
  - Outbox emission remains the source for post-commit side effects.
- **Configuration additions**
  - Added `app.retry.settlement.`*, `app.settlement.lock-timeout-ms`, and `app.kafka.*` properties.
  - Added Spring Kafka wiring and serializers in `application.yaml` plus `KafkaConfiguration`.

---

## Change log (append-only)

Use this section to record what changed, when, and why. Each entry should include:

- Date
- Step/phase
- Code/files changed
- Architecture impact
- Risks/trade-offs
- Validation performed

### Entry template

```text
Date: YYYY-MM-DD
Phase/Step: <e.g., Phase 1 - Redis rate limiting>
Summary:
- <what changed>

Files:
- <path>
- <path>

Architecture impact:
- <behavioral/operational change>

Risks / trade-offs:
- <known limitations or migration concerns>

Validation:
- <tests, smoke checks, metrics observed>
```

### Entries

- **2026-04-27**: Baseline captured in this file (no runtime behavior change).
- **2026-04-27 — Phase 1 implemented**
  - **Summary**
    - Pluggable rate limiting: **Redis** (global counters) vs **in-memory** fallback.
    - **Redis pub/sub** for odds updates so all instances refresh local `ConcurrentHashMap`.
    - **Total exposure** from DB `sum(stake * odds)` for `OPEN` bets on `getTotalExposure()`.
    - Architecture diagram added under `docs/architecture-current-phase1.jpeg`.
  - **Documentation**
    - Restructured `ARCHITECTURE_EVOLUTION.md` so the **full baseline** is preserved under *Baseline architecture* and Phase 1 is documented as **deltas**, not a replacement of baseline history.
  - **Files (representative)**
    - `pom.xml` — `spring-boot-starter-data-redis`
    - `RateLimitingFilter`, `RateLimiterGateway`, `InMemoryRateLimiterGateway`, `RedisRateLimiterGateway`
    - `RedisOddsBroadcastConfiguration`, `OddsUpdateBroadcaster`, `NoOpOddsUpdateBroadcaster`, `DefaultOddsService`
    - `BetRepository.sumExposureByStatus`, `DefaultExposureService`
    - `RedisProperties`, `application.yaml`, `application-local.yaml`, `application-cloud.yaml`
    - Tests: `InMemoryRateLimiterGatewayTest`, `RedisRateLimiterGatewayTest`, `BetRepositoryTest` (aggregate), `NoOpOddsUpdateBroadcasterTest`, updates to `RateLimitingFilterTest`, `DefaultExposureAndOddsServiceTest`, `WebMvcFilterTestSupport` + controller `@Import` updates
    - `docs/architecture-current-phase1.jpeg`
    - `ARCHITECTURE_EVOLUTION.md` (this file)
  - **Architecture impact**
    - Multi-instance deployments can enforce **one global rate limit bucket** per client key when Redis is enabled.
    - Odds propagate to **all nodes** without each read hitting Redis.
    - Reported **total exposure** aligns with persisted open bets across restarts and nodes (for code paths that call `getTotalExposure()`).
  - **Risks / trade-offs**
    - New runtime dependency: **Redis** when `app.redis.enabled=true`.
    - Odds still not a **durable shared document**; cold start / missed messages need a follow-up (Hash/DB + warm load).
  - **Validation**
    - `mvn test` (full suite, exit code 0; Docker-backed tests require Docker where enabled).
    - `@WebMvcTest` slices import `WebMvcFilterTestSupport` so `RateLimitingFilter` receives a `RateLimiterGateway` bean (slice contexts do not component-scan gateway implementations).
- **2026-04-28 — Phase 2 implemented**
  - **Summary**
    - Introduced DB-backed domain events through an outbox table and dispatcher/poller flow.
    - Added event-driven `exposure_projection` read model for deterministic global and per-user exposure.
    - Added durable canonical `latest_odds` store and shifted local map to read-through cache semantics.
    - Removed in-process exposure mutation boundary (`increaseExposure`/`decreaseExposure`) from service contract.
  - **Files (representative)**
    - Domain events: `domain/event/`*
    - Outbox: `DomainEventOutbox`, `DomainEventOutboxRepository`, `service/outbox/`*, `SchedulingConfiguration`
    - Projection: `ExposureProjection`, `ExposureProjectionKey`, `ExposureProjectionRepository`, `ExposureProjectionUpdater`, `ExposureProjectionStartupInitializer`
    - Canonical odds: `LatestOdds`, `LatestOddsId`, `LatestOddsRepository`, `DefaultOddsService`
    - Service wiring: `DefaultBetPlacementService`, `DefaultBetQueryService`, `DefaultSettlementService`, `ExposureService`, `DefaultExposureService`, `BetRepository`
    - Config/tests: `application.yaml` + updated unit/integration suites
  - **Architecture impact**
    - Stronger consistency boundary: business state change and domain event record are transactionally aligned.
    - Exposure and latest odds reads are deterministic, restart-safe, and cross-node consistent from DB-backed models.
    - Clearer command/query separation: writes emit events; reads consume projections/canonical stores.
  - **Risks / trade-offs**
    - Increased DB write/read load (outbox inserts, projection updates, poller scans).
    - More moving parts (dispatcher/poller/retry paths) and operational tuning needs (batching/indexing/cleanup).
    - Eventual lag is still possible during failures until poller catch-up completes.
  - **Validation**
    - `mvn test -Dtest=BettingServiceTest,BettingSettlementIntegrationTest,BetIdempotencyIntegrationTest` (exit code 0).
    - Updated unit tests for placement/cancel/settlement/exposure/odds boundaries (event publication + projection-based reads).
- **2026-04-28 — Phase 3 implemented**
  - **Summary**
    - Added lock-timeout-aware settlement contention handling with retries and explicit contention metrics.
    - Added idempotency observability counters for replay/race/constraint-violation patterns.
    - Introduced optional Kafka-based odds ingestion buffering with controlled consumer processing.
    - Completed rate-limiter and odds service responsibility refactors for clearer boundaries.
  - **Files (representative)**
    - Settlement reliability: `BetRepository`, `DefaultSettlementService`, `SettlementRetryConfiguration`, `SettlementRetryProperties`, `SettlementProperties`
    - Odds backpressure/refactor: `DefaultBettingService`, `DefaultOddsService`, `OddsFeedPublisher`, `DirectOddsFeedPublisher`, `KafkaOddsFeedPublisher`, `KafkaOddsFeedConsumer`, `OddsPublisher`, `DefaultOddsPublisher`, `OddsCacheUpdater`, `RedisOddsBroadcastConfiguration`, `OutboxDispatcher`
    - Rate limiting boundary: `RateLimiterGateway`, `RateLimitingFilter`, `InMemoryRateLimiterGateway`, `RedisRateLimiterGateway`
    - Config/deps: `pom.xml`, `application.yaml`, `KafkaConfiguration`
    - Tests: rate limiter tests, settlement tests, odds publisher/cache/buffer tests
  - **Architecture impact**
    - Better hot-event behavior via lock retry/timeout controls and measurable settlement contention.
    - Better operability through focused idempotency and contention metrics.
    - Optional ingestion backpressure path for odds bursts without changing external API contract.
  - **Risks / trade-offs**
    - Additional runtime complexity (Kafka optional path, extra abstractions, more configuration).
    - More operational tuning required (consumer throughput, lock timeout and retry parameters).
    - Kafka buffering introduces eventual processing lag relative to direct ingestion mode.
  - **Validation**
    - `mvn -q test-compile` (exit code 0).
    - `mvn -q test -Dtest=DefaultSettlementServiceTest,DefaultExposureAndOddsServiceTest,DefaultBettingServiceTest,InMemoryRateLimiterGatewayTest,RedisRateLimiterGatewayTest,RateLimitingFilterTest,OddsCacheUpdaterTest,DefaultOddsPublisherTest,DirectOddsFeedPublisherTest,KafkaOddsFeedPublisherTest,KafkaOddsFeedConsumerTest` (exit code 0).

