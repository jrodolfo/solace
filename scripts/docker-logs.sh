#!/usr/bin/env bash
#
# docker-logs.sh
#
# Purpose:
#   Shows logs for the full Docker runtime or for one component.
#
# Usage:
#   ./scripts/docker-logs.sh
#   ./scripts/docker-logs.sh subscriber
#   ./scripts/docker-logs.sh api
#   ./scripts/docker-logs.sh ui
#   ./scripts/docker-logs.sh mysql

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_docker_compose

cd "${REPO_ROOT}"

component="${1:-all}"

case "${component}" in
  all)
    docker compose logs -f
    ;;
  api)
    docker compose logs -f solace-broker-api
    ;;
  ui)
    docker compose logs -f solace-publisher-ui
    ;;
  subscriber)
    docker compose logs -f solace-subscriber
    ;;
  mysql)
    docker compose logs -f mysql
    ;;
  *)
    echo "unknown component: ${component}" >&2
    echo "usage: ./scripts/docker-logs.sh [all|api|ui|subscriber|mysql]" >&2
    exit 1
    ;;
esac
