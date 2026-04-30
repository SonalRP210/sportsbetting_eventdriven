# sportsbetting-eventdriven

Event-driven microservice platform for sports betting (Java/Spring Boot).

## Run Entire Stack Locally

- Start all services and local dependencies:
  - `powershell -ExecutionPolicy Bypass -File "infra/local/start-all.ps1"`
- Stop all services:
  - `powershell -ExecutionPolicy Bypass -File "infra/local/stop-all.ps1"`

## Service Ports

- `postgres`: `5433` (container port `5432`)
- `api-gateway`: `8070` (container port `8080`)
- `auth-service`: `8081`
- `user-service`: `8082`
- `wallet-service`: `8083`
- `betting-service`: `8094` (container port `8084`)
- `odds-service`: `8085`
- `event-ingestion-service`: `8086`
- `settlement-service`: `8087`
- `notification-service`: `8088`
- `risk-service`: `8089`

