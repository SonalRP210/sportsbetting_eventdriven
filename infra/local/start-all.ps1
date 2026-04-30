$ErrorActionPreference = "Stop"

docker compose -f "infra/local/docker-compose.yml" up --build -d

docker compose -f "infra/local/docker-compose.yml" ps
