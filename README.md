# sportsbetting-eventdriven

Event-driven microservice platform for sports betting (Java/Spring Boot).

## Run Entire Stack Locally

- Start all services and local dependencies:
  - `powershell -ExecutionPolicy Bypass -File "infra/local/start-all.ps1"`
- Stop all services:
  - `powershell -ExecutionPolicy Bypass -File "infra/local/stop-all.ps1"`

## Service Ports

- `postgres`: `5433` (container port `5432`)
- `kafka/redpanda`: `19092` (container port `9092`)
- `api-gateway`: `8070` (container port `8080`)
- `user-service`: `8082`
- `wallet-service`: `8083`
- `betting-service`: `8094` (container port `8084`)
- `odds-service`: `8085`
- `event-ingestion-service`: `8086`
- `settlement-service`: `8087`
- `notification-service`: `8088`
- `risk-service`: `8089`
- `prometheus`: `9090`
- `grafana`: `3000`

## Kubernetes (odds-service & betting-service)

Example manifests live under `services/odds-service/k8s/` and `services/betting-service/k8s/`. Both deployments set **`SPRING_PROFILES_ACTIVE=production`**, mount **`envFrom`** on a **required** Secret (`*-service-secrets`), and optionally an **`optional: true`** ConfigMap (`*-service-config`). Copy each service’s **`secret.example.yaml`** (and optionally **`configmap.example.yaml`**), replace placeholders, then apply the Secret before the Deployment.

**Typical Secret keys (JWT mode, default):** `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, **`JWT_ISSUER_URI`**. **API-key mode:** set **`APP_SECURITY_AUTH_TYPE=api-key`** and **`APP_SECURITY_API_KEY`** instead of relying on JWT issuer configuration. Optional header override: **`APP_SECURITY_API_KEY_HEADER`**. See also `docs/operations/production-hardening.md` and `docs/platform/vault-integration.md`.

- **odds-service** listens on container port **8080**; probes use **`/actuator/health/readiness`** and **`/actuator/health/liveness`**.
- **betting-service** listens on container port **8084**; same actuator probe paths (Service `targetPort` is the named port **`http`**).

For local Docker Desktop Kubernetes testing of the gateway, build the image into Docker Desktop, deploy it, and port-forward the Service port:

```powershell
docker build -t api-gateway:latest -f services/api-gateway/Dockerfile .
kubectl create namespace sportsbetting --dry-run=client -o yaml | kubectl apply -f -
kubectl -n sportsbetting apply -f services/api-gateway/k8s/
kubectl -n sportsbetting port-forward svc/api-gateway 8081:80
```

Additional examples (odds and betting): **`pdb.yaml`**, **`networkpolicy.example.yaml`**, **`vault-agent-injector.example.yaml`**. NetworkPolicy container ports must match the app (**8080** vs **8084**).

## HTTP API security (docker / production)

**betting-service** and **odds-service** use **`platform/service-security`**. With **`app.security.enabled=true`** (default under **`production`** and **`docker`** profiles for these services), callers must authenticate (**JWT** or **API key**). **`/actuator/health/**`** stays anonymous for probes; other **`/actuator/*`** and **`/api/**`** require the same auth unless a service-specific rule applies.

**betting-service (JWT, Keycloak realm roles):**

- **`GET /api/v1/ping`** — public (no token).
- Customer bet APIs — roles **`BET_READ`** / **`BET_PLACE`** (as applicable per endpoint).
- **`/api/v1/internal/**`** — **`BETTING_OPERATIONS`** or **`BETTING_ADMIN`**.
- Realm and role definitions: `infra/keycloak/sportsbetting-realm.json` (e.g. client **`feeder`** includes **`BET_READ`** and **`BET_PLACE`** for stack scripts and load tests through the gateway).

**API-key mode:** non–JWT-customizer paths accept **`ROLE_HTTP_SERVICE_CLIENT`** from a valid **`X-API-Key`** (same pattern as odds-service).

## Local Keycloak and realm import

Local Compose runs Keycloak with **`--import-realm`** and a bind-mounted **`infra/keycloak/sportsbetting-realm.json`**. Import runs when Keycloak initializes a **fresh** dev data directory. If you **edit the realm JSON** (for example to add **`BET_READ`** / **`BET_PLACE`** to **`feeder`**) but existing tokens still lack the new roles, **recreate the Keycloak container** so the realm is re-imported, for example:

`docker compose -f infra/local/docker-compose.yml rm -sf keycloak && docker compose -f infra/local/docker-compose.yml up -d keycloak`

Alternatively, add roles and client scope mappings in the Keycloak Admin UI. A short reminder also appears in comments on the **`keycloak`** service in `infra/local/docker-compose.yml`.

## Monitoring

- Prometheus endpoint is exposed on all services at `/actuator/prometheus`.
- When HTTP security is enabled (`production` / secured stacks), scrapes must authenticate; patterns and examples are in `docs/operations/production-hardening.md` (section **Prometheus metrics (secured actuator)**) and `docs/operations/examples/prometheus-scrape-secured-actuator.yaml`.
- Local stack includes:
  - Prometheus: [http://localhost:9090](http://localhost:9090)
  - Grafana: [http://localhost:3000](http://localhost:3000) (default login `admin` / `admin`)
- Grafana dashboards are provisioned from `observability/grafana/dashboards`.

## Event Platform Notes

- Kafka topics are created at startup by `infra/local/kafka/init-topics.sh`.
- Topic catalog lives in `messaging/kafka/topics/topic-definitions.yaml`.
- Base Postgres schemas are initialized from `data/postgres/scripts/init-schemas.sql`.

## Release Gate

- CI workflow: `.github/workflows/release-gate.yml`
- Gate stages:
  - Kubernetes manifest validation: `bash scripts/ci/k8s-manifest-dry-run.sh` (`kubectl apply --dry-run=client` on core odds/betting manifests)
  - Full reactor tests: `mvn -q test`
  - Schema compatibility checks: `bash scripts/release/schema-compatibility-check.sh`
  - Full E2E checks: `bash scripts/release/full-e2e.sh`
  - Resilience checks (broker restart): `bash scripts/release/resilience-check.sh`

## CI (Open Source Tooling)

This repository uses open-source CI checks with GitHub Actions, SonarQube, and Trivy.

- PR checks (`.github/workflows/ci-pr.yml`):
  - `Fast Tests` (`mvn test`) for unit/slice tests
  - `Integration Verify (Conditional)` (`mvn -Pquality-tools verify`) when backend-impacting files change
  - `SonarQube PR` quality gate (when `SONAR_HOST_URL` + `SONAR_TOKEN` are configured)
  - `Dependency Review`
  - `Trivy Filesystem` scan (SARIF uploaded to Security tab)
- Main checks (`.github/workflows/ci-main-quality.yml`):
  - `Full Verify` (`mvn -Pquality-tools verify`) including integration and static analysis gates
  - `SonarQube Main` quality gate (when Sonar is configured)
  - `Trivy Image Scan` matrix for service Docker images (SARIF uploaded to Security tab)
- Release checks (`.github/workflows/ci-release-artifacts.yml`):
  - `Package Artifacts` (build and upload jars)
  - `Docker Build Validation` (build-only validation for service images)

## Test Strategy Implemented

- Unit tests (JUnit5/Mockito/AssertJ):
  - `services/betting-service/.../BettingServiceUnitTest.java`
  - `services/settlement-service/.../SettlementServiceUnitTest.java`
- Slice tests:
  - `@WebMvcTest`: `services/api-gateway/.../GatewayWebMvcSliceTest.java`
  - `@DataJpaTest`: `services/betting-service/.../BetRepositoryDataJpaTest.java`
- Integration tests (Testcontainers + Awaitility):
  - `services/betting-service/.../OddsConsumerDlqIT.java`
- Contract/schema tests:
  - `platform/event-support/.../EventPayloadValidatorTest.java`
  - `services/risk-service/.../RiskEventConsumerContractTest.java`
  - `scripts/release/schema-compatibility-check.sh`
- API integration tests:
  - `services/api-gateway/.../GatewayApiIntegrationTest.java` (RestAssured)
- End-to-end/system:
  - `scripts/release/full-e2e.sh`
- Non-functional:
  - performance: `scripts/performance/k6-bets-smoke.js` (defaults to **`api-gateway`**; for secured stacks, obtain a token from Keycloak and pass **`Authorization`** through the gateway or set **`BASE_URL`** to a service base URL and send a bearer token / API key as required)
  - resilience: `scripts/resilience/chaos-broker-restart.sh`
  - security: `scripts/security/owasp-dependency-check.sh`
- CI matrix:
  - `.github/workflows/test-matrix.yml` (`unit`, `integration`, `e2e`, `non-functional`)
