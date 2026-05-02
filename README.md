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

## Monitoring

- Prometheus endpoint is exposed on all services at `/actuator/prometheus`.
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
  - Full reactor tests: `mvn -q test`
  - Schema compatibility checks: `bash scripts/release/schema-compatibility-check.sh`
  - Full E2E checks: `bash scripts/release/full-e2e.sh`
  - Resilience checks (broker restart): `bash scripts/release/resilience-check.sh`

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
  - performance: `scripts/performance/k6-bets-smoke.js`
  - resilience: `scripts/resilience/chaos-broker-restart.sh`
  - security: `scripts/security/owasp-dependency-check.sh`
- CI matrix:
  - `.github/workflows/test-matrix.yml` (`unit`, `integration`, `e2e`, `non-functional`)
