#!/usr/bin/env bash
#
# docker-status.sh
#
# Purpose:
#   Shows Docker Compose service status and checks the broker API health endpoint
#   when curl is available.
#
# Usage:
#   ./scripts/docker-status.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_docker_compose

cd "${REPO_ROOT}"

docker compose ps

if command -v curl >/dev/null 2>&1; then
  echo
  if curl -fsS "http://localhost:8081/rest/actuator/health" >/dev/null 2>&1; then
    echo "api health: UP"
  else
    echo "api health: DOWN or not ready"
  fi
fi
