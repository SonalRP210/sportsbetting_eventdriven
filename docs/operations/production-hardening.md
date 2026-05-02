# Production hardening (odds-service & platform notes)

This document captures gaps called out for production deployments that are not fully encoded as infrastructure-as-code in this repository.

## Semantics

- **HTTP `202 ACCEPTED`** on `/api/v1/odds-feed` means the service accepted the batch for chunked persistence and outbox/CDC emission — not that every downstream consumer or bet-placement flow has observed the price. Define separately whether “accepted” is sufficient for operator consoles versus guaranteed placement visibility.
- **Read-your-writes across services**: expose and honor `updatedAt` (already emitted on odds updates); consumers can compare timestamps when enforcing freshness.

## Kafka

- Local stacks often use replication factor `1`. Production clusters should use **RF ≥ 3** on topics such as `odds.updated.v1`, with enough partitions for producer/consumer parallelism.
- Confirm **`min.insync.replicas`** (typically `2` when RF is `3`), **`acks=all`**, and **retention** policies per compliance and replay requirements.
- Producer tuning is partly defaulted in `services/odds-service/src/main/resources/application.properties` (`batch-size`, `linger.ms`, compression, idempotence, retries). Adjust for measured throughput and broker limits.

## Postgres & JDBC

- The odds-service defaults include bounded ingest chunks (`app.odds.feed.chunk-size`) and Hibernate JDBC batch ordering to reduce round-trips within each chunk.
- **Flyway**: enable the `production` Spring profile (`SPRING_PROFILES_ACTIVE=production`) so migrations apply and `ddl-auto=validate`. Dev defaults keep `ddl-auto=update` with Flyway disabled.
- Scale **Hikari** (`HIKARI_*` env vars) and add **statement timeouts** at the pool or DB role level for your expected RPS.
- Plan **HA Postgres** (e.g. RDS Multi-AZ, Patroni) for production; Docker Compose uses a single node for local development only.
- For heavy **GET odds** traffic, consider read replicas or a **read-through cache** (e.g. Redis) with explicit staleness/consistency semantics.

## Outbox / Debezium / WAL

- Schema validation runs **before insert into `outbox_events`** when `app.kafka.schema-validation.enabled=true`, covering the CDC path as well as the polling dispatcher.
- Monitor **connector FAILED state**, **replication slot lag**, and **WAL disk** under spikes.
- Run Kafka Connect / connectors in **HA** or use a managed connector. **Outbox cleanup** retention is short in dev (`application.properties`); the `production` profile defaults to a longer window — align with forensics and archive-to-object-storage policies before deleting rows.

## Kubernetes

- Manifests under `services/odds-service/k8s/` use port **8080** and **Actuator** liveness/readiness paths. The sample `deployment.yaml` sets **`SPRING_PROFILES_ACTIVE=production`** (Flyway + `ddl-auto=validate`); wire **`SPRING_DATASOURCE_*`** and other secrets via your pipeline.
- A minimal **PDB** (`pdb.yaml`) and soft **topology spread** are included; tighten for your SLOs (multi-AZ, HPA bounds, pod anti-affinity).

## Observability & SLOs

- Use **tail-based or low probability** trace sampling in production (`TRACE_SAMPLE_PROBABILITY` / `management.tracing.sampling.probability` in `application-production.properties`).
- **OutboxPoller** increments `outbox.dispatch.failures` and logs at **warn** when dispatch throws (polling fallback mode).
- Sample Prometheus alert rules (scoped by **`pod`/`container`** matching the Deployment; optional **`namespace`** if needed): `infra/monitoring/prometheus/rules/odds-service.sample.yml`.
- Define SLO dashboards: ingest RPS, HTTP p95/p99, consumer lag on `odds.updated.v1`, outbox row age / cleanup volume, Postgres slot lag, HTTP 5xx.

## Disaster recovery (runbook outline)

1. **Postgres**: restore from last RPO-tested backup; replay or validate application invariants after restore.
2. **Kafka consumers**: reset or repair **consumer group offsets** only with an explicit playbook (data reprocessing risk).
3. **Debezium / Connect**: snapshot connector configs; after restore, validate **connector state** and **slot** recreation procedures against WAL continuity expectations.