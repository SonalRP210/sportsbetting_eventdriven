# Distributed systems & cross-service operations

This repository is structured for **distributed, event-driven** collaboration: services own their data, publish facts over **Kafka**, and integrate via **schemas** (`shared/contracts`) and **outbox / CDC** (e.g. odds-service → Postgres outbox → Debezium → `odds.updated.v1`). HTTP APIs are edges for commands and reads, not the only integration path.

## Reusable HTTP security (`platform/service-security`)

- **Not odds-only**: add a dependency on `**com.sportsbetting:service-security`** to any Spring Boot service and set the same `**app.security.*`** / `**JWT_ISSUER_URI**` / `**APP_SECURITY_API_KEY**` properties. Auto-configuration is registered via `**META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports**`.
- **Default remains open** (`app.security.enabled=false`) until you enable it per environment. Use the same K8s Secret / env patterns as **odds-service** and **betting-service** (`services/*/k8s/secret.example.yaml`).

## Dependency & static-analysis governance


| Mechanism                             | Where                                                                                                                                                                                      |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **OWASP Dependency-Check**            | `.github/workflows/test-matrix.yml` job `**non-functional`** → `scripts/security/owasp-dependency-check.sh`                                                                                |
| **SpotBugs, Checkstyle, PMD, JaCoCo** | Maven profile `**-Pquality-tools`** in root `**pom.xml`** (`verify` phase). CI `**integration**` job runs `**mvn -Pquality-tools verify**` (includes static analysis + ITs where present). |
| **Unit tests only**                   | `**unit`** job: `**mvn test`**                                                                                                                                                             |


Raise `**jacoco.check**` minimum over time as coverage improves.

## Transport security (operators — not automatic in Java)


| Concern          | Practice                                                                                                                                       |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **Postgres**     | JDBC URL `**sslmode=verify-full`** (or equivalent), certs from PKI / cloud RDS; secrets via `**SPRING_DATASOURCE_*`** / Secret.                |
| **Kafka**        | `**SECURITY_PROTOCOL=SASL_SSL`** (or mutual TLS), broker ACLs per producer/consumer principal; align `**SPRING_KAFKA_*`** with cluster policy. |
| **HTTP ingress** | TLS termination at ingress / gateway; optional mTLS inside mesh.                                                                               |


These are **environment** concerns: Spring picks them up via URLs and standard Kafka client properties once you configure them.

## JWT claim policies

- Baseline validation comes from Spring Security OAuth2 Resource Server (**issuer**, JWKS).
- Optional **audience** restriction: set `**spring.security.oauth2.resourceserver.jwt.audiences`** (or list form) when your IdP issues an `**aud`** claim and you want to bind tokens to this API. Omit if the IdP does not use audiences for client APIs.

## Logging discipline

- Use **structured logs** (Spring Boot default JSON with logback if you add the right encoder in your platform standard).
- **Never** log raw API keys, JWTs, or full request bodies containing PII in application code.
- `**logback-spring.xml`** on odds-service lowers framework noise under the `**production`** profile; extend the same pattern for other services.

## Outbox JPQL deletes

Bulk `**DELETE … WHERE createdAt < :cutoff**` with a **named parameter** is safe: the cutoff is bound as a parameter, not concatenated. Entity name `**OutboxEventEntity`** is compile-time JPQL, not user input. `**@Modifying(clearAutomatically = true, flushAutomatically = true)`** keeps the persistence context consistent after bulk delete. Alternatives (**Criteria API**, derived delete) are equivalent for safety; native SQL with `**?1`** binding is also fine if you prefer explicit table names.