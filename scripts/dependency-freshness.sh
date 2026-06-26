#!/usr/bin/env bash
#
# dependency-freshness.sh
#
# Purpose:
#   Reports dependency freshness for Maven, npm, and Docker image references
#   without changing project files.
#
# Usage:
#   ./scripts/dependency-freshness.sh
#
# Required tools/dependencies:
#   - bash
#   - Maven
#   - npm
#   - Docker optional for registry metadata checks
#
# Exit behavior:
#   Exits with code 0 when the report completes.
#   Exits with a non-zero code only when a required reporting tool fails.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_command mvn
require_command npm

cd "${REPO_ROOT}"

print_section() {
  echo
  echo "==================== $1 ===================="
}

run_maven_report() {
  local module_dir="$1"
  local module_name="$2"

  echo
  echo "-- ${module_name} --"
  (
    cd "${module_dir}"

    if grep -q "<parent>" pom.xml; then
      mvn versions:display-parent-updates
    fi

    mvn versions:display-dependency-updates
    mvn versions:display-plugin-updates
  )
}

run_npm_report() {
  echo
  echo "-- solace-publisher-ui --"
  (
    cd solace-publisher-ui

    set +e
    npm outdated
    local npm_status=$?
    set -e

    case "${npm_status}" in
      0)
        echo "npm dependencies are current according to npm outdated"
        ;;
      1)
        echo "npm reported outdated dependencies above"
        ;;
      *)
        echo "npm outdated failed with exit code ${npm_status}" >&2
        exit "${npm_status}"
        ;;
    esac
  )
}

collect_docker_refs() {
  {
    sed -n 's/^[[:space:]]*image:[[:space:]]*//p' docker-compose.yml
    sed -n 's/^[[:space:]]*image:[[:space:]]*//p' solace-broker-api/docker-compose.yaml
    sed -n 's/^FROM[[:space:]][[:space:]]*\([^[:space:]][^[:space:]]*\).*/\1/p' solace-broker-api/Dockerfile
    sed -n 's/^FROM[[:space:]][[:space:]]*\([^[:space:]][^[:space:]]*\).*/\1/p' solace-publisher-ui/Dockerfile
    sed -n 's/^FROM[[:space:]][[:space:]]*\([^[:space:]][^[:space:]]*\).*/\1/p' solace-subscriber/Dockerfile
  } | sed 's/[[:space:]]*$//' | sort -u
}

is_local_image_ref() {
  local image_ref="$1"
  [[ "${image_ref}" == *":local" ]]
}

image_tag() {
  local image_ref="$1"
  local image_without_digest="${image_ref%@*}"
  local final_segment="${image_without_digest##*/}"

  if [[ "${final_segment}" == *":"* ]]; then
    printf '%s\n' "${final_segment##*:}"
  else
    printf '%s\n' ""
  fi
}

run_docker_report() {
  local can_inspect="false"
  if command -v docker >/dev/null 2>&1 && docker buildx version >/dev/null 2>&1; then
    can_inspect="true"
  fi

  echo
  echo "-- image references --"

  local image_ref
  while IFS= read -r image_ref; do
    [[ -n "${image_ref}" ]] || continue

    echo "${image_ref}"

    if is_local_image_ref "${image_ref}"; then
      echo "  project-local image tag; freshness comes from its Dockerfile base images"
      continue
    fi

    local tag
    tag="$(image_tag "${image_ref}")"

    if [[ -z "${tag}" ]]; then
      echo "  warning: image reference has no explicit tag"
    elif [[ "${tag}" == "latest" ]]; then
      echo "  warning: moving latest tag; pin this to a release tag for reproducibility"
    else
      echo "  pinned tag: ${tag}"
    fi

    if [[ "${can_inspect}" == "true" ]]; then
      if docker buildx imagetools inspect "${image_ref}" >/dev/null; then
        echo "  registry metadata: available"
      else
        echo "  registry metadata: unavailable"
      fi
    else
      echo "  registry metadata: skipped; docker buildx is unavailable"
    fi
  done < <(collect_docker_refs)
}

echo "dependency freshness report"
echo "policy: report only; no dependency files are modified"

print_section "Maven"
run_maven_report "solace-broker-api" "solace-broker-api"
run_maven_report "solace-subscriber" "solace-subscriber"

print_section "npm"
run_npm_report

print_section "Docker images"
run_docker_report

echo
echo "dependency freshness report complete"
