#!/usr/bin/env bash
set -euo pipefail

# Generates dependency vulnerability report for Java modules.
mvn -B -q org.owasp:dependency-check-maven:check \
  -Dformat=HTML \
  -DskipProvidedScope=true \
  -DskipTestScope=false

echo "OWASP dependency-check completed."
