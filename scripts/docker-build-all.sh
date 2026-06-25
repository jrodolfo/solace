#!/usr/bin/env bash
#
# docker-build-all.sh
#
# Purpose:
#   Builds or pulls all images used by the Docker runtime without starting
#   containers.
#
# Usage:
#   ./scripts/docker-build-all.sh
#
# Required tools/dependencies:
#   - bash
#   - docker with the Compose plugin

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_docker_compose

cd "${REPO_ROOT}"

echo "pulling Docker runtime base images"
docker compose pull mysql

echo
echo "building Docker runtime application images"
docker compose build solace-broker-api solace-subscriber
docker compose build --no-cache solace-publisher-ui

echo
echo "Docker runtime images are ready"
