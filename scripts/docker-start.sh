#!/usr/bin/env bash
#
# docker-start.sh
#
# Purpose:
#   Builds and starts the full Docker runtime: MySQL, broker API, publisher UI,
#   and subscriber.
#
# Usage:
#   ./scripts/docker-start.sh
#
# Required tools/dependencies:
#   - bash
#   - docker with the Compose plugin
#   - Solace environment variables (validated by common.sh).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_docker_compose
require_solace_env_vars "docker-start.sh"

cd "${REPO_ROOT}"

echo "building and starting Docker runtime"
docker compose up --build -d

echo
echo "docker runtime started"
echo "api health: http://localhost:8081/rest/actuator/health"
echo "api docs:   http://localhost:8081/docs"
echo "ui:         http://localhost:5173"
echo "mysql:      localhost:3307"
