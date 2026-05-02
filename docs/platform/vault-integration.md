# HashiCorp Vault (secrets + TLS) with api-gateway → odds-service

This repo assumes **machine-to-machine** traffic: an upstream system calls **api-gateway**, which proxies to **odds-service** and other backends (for example **betting-service**). End-user login stays **out of scope** (existing gateway `/auth/login` stub is unchanged). **betting-service** mirrors **odds-service** for Vault-enabled builds and KV layout.

## 1. Secrets (Vault KV + Spring)

**Goal:** Keep database passwords, Kafka credentials, `APP_SECURITY_API_KEY`, `JWT_ISSUER_URI`, etc. out of Kubernetes `Secret` manifests and git.

**Application changes (already in tree):**

- **odds-service** and **betting-service** can include **`spring-cloud-starter-vault-config`** by building with Maven profile **`-Pvault`** (dependency is not on the classpath by default, so local tests and stacks without Vault stay simple).
- Root POM **`dependencyManagement`** imports **`spring-cloud-dependencies` 2023.0.5** for aligned Cloud stack versions when the profile is used.
- **`application-vault.properties`** (per service) enables Vault **Kubernetes auth** and **KV** reads when Spring profile **`vault`** is active (`SPRING_PROFILES_ACTIVE=production,vault`). Default Kubernetes role names: **`VAULT_K8S_ROLE`** `odds-service` or `betting-service` (override per deployment).
- **`spring.cloud.vault.enabled=false`** remains in each service’s default **`application.properties`** when the Vault starter is present.

**Build Vault-enabled jar/image:** `mvn package -pl services/odds-service -Pvault` and/or `mvn package -pl services/betting-service -Pvault` (or activate **`vault`** in your Docker layer).

**Runtime:** set profiles to include **`vault`** together with **`production`**, for example:

`SPRING_PROFILES_ACTIVE=production,vault`

**Vault operator work:**

1. Enable **`kubernetes`** auth and bind each workload **ServiceAccount** to a Vault role (least privilege), e.g. **odds-service** and **betting-service**.
2. Store KV v2 data under your backend (default **`secret`**) using Spring-friendly keys, for example at  
   `secret/data/odds-service/production` and `secret/data/betting-service/production`:

   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_KAFKA_BOOTSTRAP_SERVERS` (if not in ConfigMap)
   - `JWT_ISSUER_URI` (JWT mode) **or** `APP_SECURITY_API_KEY` (API-key mode)
   - Any other `SPRING_*` / `APP_*` variables you currently put in `k8s/secret.example.yaml`

3. Optionally **remove** redundant keys from `envFrom.secretRef` once Vault supplies them (keep ConfigMap for non-secrets).

## 2. Certificates (Vault PKI / Agent files)

Vault does **not** replace Spring’s TLS wiring; it **delivers** PEM or keystore material that Spring consumes.

### odds-service as HTTPS server

Mount PEM paths written by **Vault Agent** (or CSI) and enable Boot TLS, for example:

```properties
server.ssl.bundle.pem.odds-server.keystore.certificate=file:/vault/secrets/server.crt
server.ssl.bundle.pem.odds-server.keystore.private-key=file:/vault/secrets/server.key
server.ssl.bundle.pem.odds-server.truststore.certificate=file:/vault/secrets/ca-chain.pem
server.ssl.bundle.name=odds-server
```

(Exact property names follow [Spring Boot SSL bundles](https://docs.spring.io/spring-boot/reference/features/ssl.html); align with your Boot version.)

### api-gateway → odds-service (HTTPS / mTLS)

**Application changes (already in tree):**

- **`GatewayRestClientConfig`** applies an SSL bundle to the outbound **`RestClient`** when **`gateway.downstream.ssl.bundle-name`** is set (delegates to Spring Boot’s **`RestClientSsl.fromBundle(name)`**).
- Example PEM bundle wiring is commented in **`services/api-gateway/src/main/resources/application.properties`** (`gateway-downstream` bundle + trust/client certs).

**Operator work:** Issue server certs for odds-service and optional **client** certs for the gateway via Vault **PKI**, render PEM files with Agent templates into `/vault/secrets/`, then point **`spring.ssl.bundle.pem.*`** at those paths.

## 3. Auth headers through the gateway

When **`service-security`** is enabled on **odds-service** or **betting-service**, callers must send **`Authorization: Bearer …`** (JWT) or **`X-API-Key`** (API-key mode).

**Application changes (already in tree):**

- The gateway **forwards** those headers on proxied routes via **`DownstreamAuthHeaders`** so  
  `system → api-gateway → odds-service` preserves credentials **without** putting secrets in gateway config.

## 4. Kubernetes references

- Annotate each Pod for **Vault Agent Injector** or use **Vault CSI** — store templates next to your Helm/Kustomize (see HashiCorp docs for your chart version).
- Example snippets: **`services/odds-service/k8s/vault-agent-injector.example.yaml`**, **`services/betting-service/k8s/vault-agent-injector.example.yaml`** (merge into your real Deployments).

## 5. Summary checklist

| Layer | What you configure |
| ----- | ------------------- |
| Vault | KV secrets, PKI roles, Kubernetes auth, policies |
| odds-service / betting-service Pods | Agent/CSI volumes, `SPRING_PROFILES_ACTIVE=production,vault`, PEM paths for server SSL where used |
| api-gateway Pod | PEM bundle for trust (+ client cert for mTLS), `gateway.downstream.ssl.bundle-name`, downstream route URLs |
| Network | Restrict who can reach gateway and backends; optional NetworkPolicy unchanged in principle |
