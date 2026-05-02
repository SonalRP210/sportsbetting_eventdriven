# Production hardening (odds-service & platform notes)

This document captures gaps called out for production deployments that are not fully encoded as infrastructure-as-code in this repository.

## HTTP / application security (odds-service)

- **Spring Security** via shared library **`platform/service-security`** (`com.sportsbetting:service-security`): **off** for the default profile (`app.security.enabled=false`); **`production`** sets **`app.security.enabled=true`**. See `docs/platform/distributed-systems-and-operations.md` to reuse in other services.
- **JWT (default in production)** — OAuth2 Resource Server: set **`JWT_ISSUER_URI`** (maps to **`spring.security.oauth2.resourceserver.jwt.issuer-uri`**) to your IdP issuer (e.g. Keycloak realm issuer URL). Clients send **`Authorization: Bearer <access_token>`**. Actuator **`/actuator/health/**`** stays **anonymous** for Kubernetes probes; **`/actuator/prometheus`**, **`/actuator/metrics`**, **`/actuator/info`** require the same authentication as **`/api/**`** (configure Prometheus scrape with a bearer token or sit behind a mesh that injects credentials).
- **API key (alternative)** — set **`APP_SECURITY_AUTH_TYPE=api-key`** and **`APP_SECURITY_API_KEY`** (Secret). Clients send **`X-API-Key`** (override name with **`APP_SECURITY_API_KEY_HEADER`** / **`app.security.api-key-header`**). Prefer JWT for humans and long-lived platform integrations when an IdP is available.
- Keep **`/api/v1/internal/**`** off public ingress and enforce **NetworkPolicy** / gateway rules regardless of in-app auth.

## Semantics

- **HTTP `202 ACCEPTED`** on `/api/v1/odds-feed` means the service accepted the batch for chunked persistence and outbox/CDC emission — not that every downstream consumer or bet-placement flow has observed the price. Define separately whether “accepted” is sufficient for operator consoles versus guaranteed placement visibility.
- **Read-your-writes across services**: expose and honor `updatedAt` (already emitted on odds updates); consumers can compare timestamps when enforcing freshness.

## Kafka

- Local stacks often use replication factor `1`. Production clusters should use **RF ≥ 3** on topics such as `odds.updated.v1`, with enough partitions for producer/consumer parallelism.
- Confirm `**min.insync.replicas`** (typically `2` when RF is `3`), `**acks=all`**, and **retention** policies per compliance and replay requirements.
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

- Manifests under `services/odds-service/k8s/` assume **`SPRING_PROFILES_ACTIVE=production`**. The **`Deployment`** mounts **`envFrom.secretRef`** on **`odds-service-secrets`** (required): copy **`secret.example.yaml`**, replace placeholders, apply **`kubectl apply -f odds-service-secrets.yaml`** before **`deployment.yaml`**. Include **`JWT_ISSUER_URI`** (JWT auth) **or** **`APP_SECURITY_AUTH_TYPE=api-key`** + **`APP_SECURITY_API_KEY`** from your vault pipeline.
- Optional **`ConfigMap`** **`odds-service-config`**: see **`configmap.example.yaml`** (referenced as **`optional: true`**).
- Pods run as **UID/GID 10001**, **read-only root filesystem**, **`emptyDir`** **`/tmp`** for JVM temp files (see **`Dockerfile`**).
- **`networkpolicy.example.yaml`** shows ingress scoped to an **`ingress-nginx`** namespace label — customize before enabling.
- Wire **`SPRING_DATASOURCE_*`** and Kafka endpoints via Secret or ConfigMap as fits your standards.
- **Actuator** probes use **`/actuator/health/liveness`** and **`readiness`** on port **8080**. **`app.features.seed-odds-enabled=false`** turns off the optional **`POST /api/v1/internal/seed-odds`** bean — keep **`/internal/**`** off public ingress regardless.
- A minimal **PDB** (`pdb.yaml`) and soft **topology spread** are included; tighten for your SLOs (multi-AZ, HPA bounds, pod anti-affinity).

## Observability & SLOs

- Use **tail-based or low probability** trace sampling in production (`TRACE_SAMPLE_PROBABILITY` / `management.tracing.sampling.probability` in `application-production.properties`).
- **OutboxPoller** increments `outbox.dispatch.failures` and logs at **warn** when dispatch throws (polling fallback mode).
- Prometheus alert rules (scoped by `**pod`/`container`** matching the Deployment; optional `**namespace`** if needed): `infra/monitoring/prometheus/rules/odds-service.rules.yml`.
- Define SLO dashboards: ingest RPS, HTTP p95/p99, consumer lag on `odds.updated.v1`, outbox row age / cleanup volume, Postgres slot lag, HTTP 5xx.

## Disaster recovery (runbook outline)

1. **Postgres**: restore from last RPO-tested backup; replay or validate application invariants after restore.
2. **Kafka consumers**: reset or repair **consumer group offsets** only with an explicit playbook (data reprocessing risk).
3. **Debezium / Connect**: snapshot connector configs; after restore, validate **connector state** and **slot** recreation procedures against WAL continuity expectations.

