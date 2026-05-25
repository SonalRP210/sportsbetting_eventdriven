# Sportsbetting mTLS lab integration

This repo contains the application services. The local mTLS troubleshooting lab is split across sibling repos:

- `sportsbetting-eventdriven` / `sportsbetting_eventdriven`: gateway, odds, and betting services.
- `devops-platform`: Docker Compose stacks, startup scripts, Vault dev server, and local runbooks.
- `observability-platform`: Prometheus scrape config, alert rules, and Grafana dashboards.
- `mtls-secret-rotation`: Spring Boot starter, Vault seed scripts, and chaos scripts.

Expected local layout:

```text
D:\github personal\
  sportsbetting-eventdriven\
  devops-platform\
  observability-platform\
  mtls-secret-rotation\
```

## mTLS edges

```text
external caller -> api-gateway:8080
api-gateway -> betting-service:8443  (gateway is mTLS client, betting-service is mTLS server)
api-gateway -> odds-service:8443     (gateway is mTLS client, odds-service is mTLS server)
```

The normal HTTP app ports remain available for health and local checks:

| Service | App port in container | Management port in mTLS lab | mTLS port in container |
| --- | ---: | ---: | ---: |
| `api-gateway` | 8080 | same as app | n/a client only |
| `betting-service` | 8084 | 8085 | 8443 |
| `odds-service` | 8080 | 8081 | 8443 |

## Dashboard path

Prometheus scrapes `/actuator/prometheus` from the management ports and Grafana reads the `mTLS Secret Rotation` dashboard from `observability-platform/grafana/dashboards/mtls-rotation.json`.

Required mTLS series:

```text
mtls_ssl_context_ready
mtls_reload_total
mtls_validation_failure_total
mtls_cert_days_until_expiry
mtls_cert_not_after_timestamp_seconds
```

If these series are missing, first confirm the images were built with `services/*/Dockerfile.mtls` and the `mtls-secret-rotation` Maven starter was installed during the Docker build.

## Quick checks

From `devops-platform`:

```powershell
.\environments\dev\local\start-mtls-loadtest.ps1
```

Then verify Prometheus has mTLS metrics:

```powershell
curl.exe -s "http://localhost:9090/api/v1/query?query=mtls_ssl_context_ready"
curl.exe -s "http://localhost:9090/api/v1/targets" | findstr mtls
```

Open Grafana:

```text
http://localhost:3000/d/mtls-rotation-lab
```

## Failure classification

When troubleshooting `SSLHandshakeException`, `certificate_unknown`, `bad_certificate`, or connection resets on `:8443`, identify the failing edge first:

| Edge | Client material | Server material |
| --- | --- | --- |
| gateway -> betting | gateway client cert + CA trust | betting server cert + gateway client CA trust |
| gateway -> odds | gateway client cert + CA trust | odds server cert + gateway client CA trust |

Intermittent failures after deploys or Vault writes usually indicate one side reloaded a new certificate/CA before the other side trusted it. Use the Grafana dashboard to check reload failures, validation failures, and certificate expiry.
