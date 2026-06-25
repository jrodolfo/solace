#!/usr/bin/env bash
#
# docker-start.sh
#
# Purpose:
#   Starts the full Docker runtime: MySQL, broker API, publisher UI, and
#   subscriber. Images are prepared by docker-build-all.sh first.
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

echo "preparing Docker runtime images"
"${SCRIPT_DIR}/docker-build-all.sh"

echo
echo "starting Docker runtime"
docker compose up -d --force-recreate

echo
echo "docker runtime started"
echo "api health: http://localhost:8081/rest/actuator/health"
echo "api docs:   http://localhost:8081/docs"
echo "ui:         http://localhost:5173"
echo "mysql:      localhost:3307"
echo
echo "subscriber logs:"
echo "  ./scripts/docker-logs.sh subscriber"
echo
echo "all logs:"
echo "  ./scripts/docker-logs.sh"
echo
echo "Docker Desktop:"
echo "  Containers > solace > solace-subscriber > Logs"
