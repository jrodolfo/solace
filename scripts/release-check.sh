#!/usr/bin/env bash
#
# release-check.sh
#
# Purpose:
#   Runs the pre-release validation gate from one command.
#
# Usage:
#   ./scripts/release-check.sh
#
# Required tools/dependencies:
#   - bash
#   - make
#   - Java 21 and Maven
#   - Node.js and npm
#   - Docker with the Compose plugin
#   - trivy
#
# Exit behavior:
#   Exits with code 0 on success.
#   Exits with a non-zero code as soon as a validation step fails.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_command make

cd "${REPO_ROOT}"

run_step() {
  local title="$1"
  shift

  echo
  echo "==================== ${title} ===================="
  "$@"
}

echo "release validation gate"
echo "scope: tests, builds, Docker image build, Docker image scan"

run_step "script smoke tests" make test-scripts
run_step "broker api tests" make test-api
run_step "publisher ui tests" make test-ui
run_step "subscriber tests" make test-subscriber
run_step "docker runtime image build" make docker-build-all
run_step "docker runtime image scan" make docker-scan

echo
echo "release validation passed"
