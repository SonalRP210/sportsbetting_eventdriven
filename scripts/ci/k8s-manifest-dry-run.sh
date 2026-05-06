#!/usr/bin/env bash
# Client-side validation of core Kubernetes manifests (no cluster required).
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
echo "Using repo root: $ROOT"

validate() {
  local f="$1"
  echo "kubectl apply --dry-run=client -f $f"
  kubectl apply --dry-run=client -f "$f"
}

# Shippable resources only (exclude *.example.yaml and comment-only snippets).
validate "$ROOT/services/odds-service/k8s/deployment.yaml"
validate "$ROOT/services/odds-service/k8s/service.yaml"
validate "$ROOT/services/odds-service/k8s/hpa.yaml"
validate "$ROOT/services/odds-service/k8s/pdb.yaml"

validate "$ROOT/services/betting-service/k8s/deployment.yaml"
validate "$ROOT/services/betting-service/k8s/service.yaml"
validate "$ROOT/services/betting-service/k8s/hpa.yaml"
validate "$ROOT/services/betting-service/k8s/pdb.yaml"

echo "k8s manifest dry-run OK."
