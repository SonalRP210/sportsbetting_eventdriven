# Production hardening (odds-service, betting-service & platform notes)

This document captures gaps called out for production deployments that are not fully encoded as infrastructure-as-code in this repository.

## HTTP / application security (odds-service & betting-service)

- **Spring Security** via shared library `**platform/service-security`** (`com.sportsbetting:service-security`): **off** for the default profile (`app.security.enabled=false`); `**production`** sets `**app.security.enabled=true**` for odds and betting. See `docs/platform/distributed-systems-and-operations.md` to reuse in other services.
- **JWT (default in production)** — OAuth2 Resource Server: set `**JWT_ISSUER_URI**` (maps to `**spring.security.oauth2.resourceserver.jwt.issuer-uri**`) to your IdP issuer (e.g. Keycloak realm issuer URL). Clients send `**Authorization: Bearer <access_token>**`. Actuator `**/actuator/health/****` stays **anonymous** for Kubernetes probes; `**/actuator/prometheus**`, `**/actuator/metrics**`, `**/actuator/info**` require the same authentication as `**/api/****`. See **Prometheus metrics (secured actuator)** below; fragment: `docs/operations/examples/prometheus-scrape-secured-actuator.yaml`.
- **API key (alternative)** — set `**APP_SECURITY_AUTH_TYPE=api-key**` and `**APP_SECURITY_API_KEY**` (Secret). Clients send `**X-API-Key**` (override name with `**APP_SECURITY_API_KEY_HEADER**` / `**app.security.api-key-header**`). Prefer JWT for humans and long-lived platform integrations when an IdP is available.
- **betting-service (JWT RBAC)** — `**GET /api/v1/ping**` is public. Customer-facing bet APIs require realm roles `**BET_READ**` / `**BET_PLACE**`; `**/api/v1/internal/****` requires `**BETTING_OPERATIONS**` or `**BETTING_ADMIN**`. Align Keycloak (or your IdP) with `infra/keycloak/sportsbetting-realm.json`.
- Keep `**/api/v1/internal/****` off public ingress and enforce **NetworkPolicy** / gateway rules regardless of in-app auth.

## Semantics

- **HTTP 202 ACCEPTED** on `/api/v1/odds-feed` means the service accepted the batch for chunked persistence and outbox/CDC emission — not that every downstream consumer or bet-placement flow has observed the price. Define separately whether “accepted” is sufficient for operator consoles versus guaranteed placement visibility.
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

- Manifests under `**services/odds-service/k8s/**` and `**services/betting-service/k8s/**` assume `**SPRING_PROFILES_ACTIVE=production**`. Each `**Deployment**` mounts `**envFrom.secretRef**` on `**odds-service-secrets**` / `**betting-service-secrets**` (required): copy the service’s `**secret.example.yaml**`, replace placeholders, apply the Secret before `**deployment.yaml**`. Include `**JWT_ISSUER_URI**` (JWT auth) **or** `**APP_SECURITY_AUTH_TYPE=api-key**` + `**APP_SECURITY_API_KEY**` from your vault pipeline.
- Optional `**ConfigMap`**s `**odds-service-config`** / `**betting-service-config**`: see each service’s `**configmap.example.yaml**` (referenced as `**optional: true**`).
- Pods run as **UID/GID 10001**, **read-only root filesystem**, `**emptyDir`** `**/tmp**` for JVM temp files (see each service’s `**Dockerfile**`).
- `**networkpolicy.example.yaml**` in each service shows **ingress** scoped to an `**ingress-nginx**` namespace label — customize before enabling. Use container port **8080** (odds) or **8084** (betting) in the policy `**ports`** entry. For production, add **egress** rules (DNS, Kafka, Postgres, IdP, Vault) explicit to your CNI and topology; the examples omit egress so local and permissive clusters keep working until you tighten.
- Wire `**SPRING_DATASOURCE_***` and Kafka endpoints via Secret or ConfigMap as fits your standards.
- **Actuator** probes use `**/actuator/health/liveness`** and `**readiness**`: port **8080** (odds) or **8084** (betting). `**app.features.seed-odds-enabled=false`** turns off the optional `**POST /api/v1/internal/seed-odds**` bean — keep `**/internal/****` off public ingress regardless.
- A minimal **PDB** (`pdb.yaml`) and soft **topology spread** are included per service; tighten for your SLOs (multi-AZ, HPA bounds, pod anti-affinity).

## Observability & SLOs

- Use **tail-based or low probability** trace sampling in production (`TRACE_SAMPLE_PROBABILITY` / `management.tracing.sampling.probability` in `application-production.properties`).
- **OutboxPoller** increments `outbox.dispatch.failures` and logs at **warn** when dispatch throws (polling fallback mode).
- Prometheus alert rules (scoped by `**pod`/`container`** matching the Deployment; optional `**namespace`** if needed): `infra/monitoring/prometheus/rules/odds-service.rules.yml`.
- Define SLO dashboards: ingest RPS, HTTP p95/p99, consumer lag on `odds.updated.v1`, outbox row age / cleanup volume, Postgres slot lag, HTTP 5xx.
- Import `docs/operations/examples/grafana-kafka-consumer-group-lag-dashboard.json` into Grafana, or copy it into the observability platform dashboard provisioning directory, to show Kafka consumer group lag by group, topic, and partition. It expects Kafka exporter metrics such as `kafka_consumergroup_lag` to be available in Prometheus.

## Prometheus metrics (secured actuator)

With **`app.security.enabled=true`**, **`/actuator/prometheus`** is **not** anonymous. Pick one operational pattern (often combined):

- **Bearer scrape** — Give Prometheus a token acceptable to the resource server (machine client from Keycloak, or a dedicated JWT minted for metrics only with minimal roles). In static Prometheus, use **`bearer_token_file`** or **`oauth2`** on the scrape config. See `docs/operations/examples/prometheus-scrape-secured-actuator.yaml`.
- **Metrics sidecar** — Run a small exporter or OpenTelemetry collector **inside the pod** that listens on **localhost** (no cluster auth), scrapes the app with a local secret or loopback-only path, and exposes a **separate unauthenticated `/metrics`** only where your NetworkPolicy allows (e.g. monitoring namespace). Keeps the main app port fully authenticated.
- **In-mesh scrape** — Service mesh (Istio, Linkerd, Cilium) or **SPIFFE**-backed identities: Prometheus (or the vendor metrics stack) scrapes over **mTLS**; policies restrict which workload identity may call **`/actuator/prometheus`**. This addresses **service-to-service trust beyond JWT at the edge** for observability paths as well as RPC.
- **Dedicated metrics path** — Some teams expose **`/internal/metrics`** on a second server port bound to **Pod IP only** or **Service without ingress**; still protect with NetworkPolicy and/or auth. Spring Boot can run a **separate management server port** (`management.server.port`) so you can firewall “main API + secured actuator” differently from “internal metrics port” — document which port your Prometheus targets.

**Kubernetes RBAC:** Prefer a **dedicated ServiceAccount** for Prometheus (or Thanos/Mimir agent) with **projected tokens** or **TokenRequest** short TTL, and bind only the **roles** your IdP expects for the metrics client — not cluster-admin.

## HashiCorp Vault (runtime secrets)

**odds-service** and **betting-service** can include **`spring-cloud-starter-vault-config`** via Maven **`-Pvault`**. Default **`spring.cloud.vault.enabled=false`**; activate **`SPRING_PROFILES_ACTIVE=production,vault`** and configure Vault **Kubernetes auth** and KV paths (`secret/data/odds-service/production`, `secret/data/betting-service/production`). Build: `mvn package -pl services/odds-service -Pvault` and `mvn package -pl services/betting-service -Pvault`. Full checklist: `docs/platform/vault-integration.md`.

## CI guardrails (manifests)

Pull requests run **`scripts/ci/k8s-manifest-dry-run.sh`** (see `.github/workflows/test-matrix.yml`): **`kubectl apply --dry-run=client`** on core **`Deployment`**, **`Service`**, **`HPA`**, **`PDB`** for odds and betting. This catches invalid YAML/kinds without a live cluster.

## Transport identity (mTLS / mesh / SPIFFE)

**JWT at the HTTP edge** validates callers to public APIs; **east-west** traffic (gateway → service, service → Kafka/Postgres) often still uses plaintext inside the cluster unless you add:

- A **service mesh** (automatic mTLS between injected sidecars, L7 policy, optional JWT pass-through).
- **SPIFFE** / **SPIRE** (workload X.509 SVIDs) for mutual TLS or JWT-SVID between services and data planes.
- **Network policies + private PKI** for narrower blast radius when a mesh is not adopted.

This repository does not ship a mesh; treat the above as the standard next step when threat models require **zero-trust** between pods.

## SLOs, error budgets, chaos, and event-path alerts

- **SLOs** — Define SLIs (availability, successful bet placement rate, odds ingest latency, settlement lag). Attach **error budgets** to each SLO and gate risky releases when burn rate is high.
- **Consumer lag** — Alert on **Kafka consumer group lag** (p95/max) per critical topic; tune thresholds per partition count and traffic.
- **Outbox age** — Track **time since insert** for outbox rows (or CDC lag vs. wall clock); alert when age exceeds an SLA that implies delayed fan-out.
- **Debezium / slot lag** — First-class alerts on **replication slot lag** and connector **FAILED** state (see **Outbox / Debezium / WAL** above).
- **Chaos / game days** — Run controlled experiments (broker restart scripts exist under `scripts/resilience/`; extend with pod kills, partition loss, IdP outage) and record blast radius against your runbooks.

## Contract tests and schema registry discipline

- **Every critical topic** should have an **owned schema** (Avro/JSON Schema per your stack), **compatibility rules** enforced in CI (see `scripts/release/schema-compatibility-check.sh` and contract-style tests under services), and **producer/consumer contract tests** so breaking changes fail before deploy.
- **Document** subject naming, evolution policy (BACKWARD vs FULL), and who approves **major** version bumps.
- Align **Schema Registry** environment URLs and credentials with the same Secret/Vault patterns as application JDBC and Kafka.

## Disaster recovery (runbook outline)

1. **Postgres**: restore from last RPO-tested backup; replay or validate application invariants after restore.
2. **Kafka consumers**: reset or repair **consumer group offsets** only with an explicit playbook (data reprocessing risk).
3. **Debezium / Connect**: snapshot connector configs; after restore, validate **connector state** and **slot** recreation procedures against WAL continuity expectations.