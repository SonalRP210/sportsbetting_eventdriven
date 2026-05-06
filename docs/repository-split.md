# Repository Split

This repository now focuses on application/domain code.

Operational and observability assets were extracted to:

- DevOps platform repo: [devops-platform](https://github.com/SonalRP210/devops-platform)
- Observability platform repo: [observability-platform](https://github.com/SonalRP210/observability-platform)

## Moved Areas

- `infra/` -> moved to `devops-platform` (`platform/`, `environments/`, `scripts/`)
- `scripts/` -> moved to `devops-platform` (`loadtests/`, `scripts/`)
- `observability/` -> moved to `observability-platform` (`grafana/`, `prometheus/`, `loki/`, `tempo/`)
