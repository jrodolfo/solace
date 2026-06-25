#!/usr/bin/env bash
#
# docker-stop.sh
#
# Purpose:
#   Stops the full Docker runtime created by docker-start.sh.
#
# Usage:
#   ./scripts/docker-stop.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_docker_compose

cd "${REPO_ROOT}"

echo "stopping Docker runtime"
docker compose down
