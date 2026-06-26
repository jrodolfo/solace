#!/usr/bin/env bash
#
# docker-scan.sh
#
# Purpose:
#   Scans the Docker runtime images with Trivy.
#
# Usage:
#   ./scripts/docker-scan.sh
#   ./scripts/docker-scan.sh --full
#
# Required tools/dependencies:
#   - bash
#   - docker with the Compose plugin
#   - trivy
#
# Exit behavior:
#   Default mode fails when Trivy finds HIGH or CRITICAL vulnerabilities.
#   Full mode reports LOW, MEDIUM, HIGH, and CRITICAL findings without failing.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

usage() {
  echo "usage: ./scripts/docker-scan.sh [--full]" >&2
}

scan_mode="release"
severity="HIGH,CRITICAL"
exit_code="1"
ignore_unfixed="true"

if [[ $# -gt 1 ]]; then
  usage
  exit 1
fi

if [[ $# -eq 1 ]]; then
  case "$1" in
    --full)
      scan_mode="full"
      severity="LOW,MEDIUM,HIGH,CRITICAL"
      exit_code="0"
      ignore_unfixed="false"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
fi

require_docker_compose
require_command trivy

cd "${REPO_ROOT}"

services=(
  "mysql:mysql:8.4"
  "solace-broker-api:solace-broker-api:local"
  "solace-publisher-ui:solace-publisher-ui:local"
  "solace-subscriber:solace-subscriber:local"
)

echo "docker image security scan"
echo "mode: ${scan_mode}"
echo "severity: ${severity}"
if [[ "${scan_mode}" == "release" ]]; then
  echo "policy: fail on fixed HIGH or CRITICAL vulnerabilities"
else
  echo "policy: report all severities without failing"
fi
echo

scan_failures=0

for service_entry in "${services[@]}"; do
  service="${service_entry%%:*}"
  image_ref="${service_entry#*:}"

  echo "==================== ${service} ===================="

  if ! docker image inspect "${image_ref}" >/dev/null 2>&1; then
    echo "missing local image for service: ${service}" >&2
    echo "expected image: ${image_ref}" >&2
    echo "run ./scripts/docker-build-all.sh or ./scripts/docker-start.sh before scanning" >&2
    scan_failures=$((scan_failures + 1))
    echo
    continue
  fi

  trivy_args=(image --severity "${severity}" --exit-code "${exit_code}")
  if [[ "${ignore_unfixed}" == "true" ]]; then
    trivy_args+=(--ignore-unfixed)
  fi
  trivy_args+=("${image_ref}")

  if ! trivy "${trivy_args[@]}"; then
    scan_failures=$((scan_failures + 1))
  fi

  echo
done

if [[ ${scan_failures} -gt 0 ]]; then
  echo "docker image security scan failed: ${scan_failures} image(s) need attention" >&2
  exit 1
fi

echo "docker image security scan passed"
