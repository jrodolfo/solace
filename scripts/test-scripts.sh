#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

assert_contains() {
  local haystack="$1"
  local needle="$2"

  if [[ "${haystack}" != *"${needle}"* ]]; then
    echo "expected output to contain: ${needle}" >&2
    echo "actual output:" >&2
    echo "${haystack}" >&2
    exit 1
  fi
}

assert_command_fails_with() {
  local expected_text="$1"
  shift

  set +e
  local output
  output="$("$@" 2>&1)"
  local exit_code=$?
  set -e

  if [[ ${exit_code} -eq 0 ]]; then
    echo "expected command to fail: $*" >&2
    exit 1
  fi

  assert_contains "${output}" "${expected_text}"
}

echo "checking make help output"
make_help_output="$(make -C "${REPO_ROOT}" help)"
assert_contains "${make_help_output}" "make build-api"
assert_contains "${make_help_output}" "make build-all"
assert_contains "${make_help_output}" "make start-api"
assert_contains "${make_help_output}" "make status-all"
assert_contains "${make_help_output}" "make test-scripts"

echo "checking shell script syntax"
bash -n \
  "${REPO_ROOT}/scripts/common.sh" \
  "${REPO_ROOT}/scripts/start-broker-api.sh" \
  "${REPO_ROOT}/scripts/start-publisher-ui.sh" \
  "${REPO_ROOT}/scripts/start-subscriber.sh" \
  "${REPO_ROOT}/scripts/start-all.sh" \
  "${REPO_ROOT}/scripts/status-all.sh" \
  "${REPO_ROOT}/scripts/build-broker-api.sh" \
  "${REPO_ROOT}/scripts/build-publisher-ui.sh" \
  "${REPO_ROOT}/scripts/build-subscriber.sh" \
  "${REPO_ROOT}/scripts/build-all.sh"

echo "checking status-all output structure"
status_all_output="$("${REPO_ROOT}/scripts/status-all.sh")"
assert_contains "${status_all_output}" "==================== workspace status ===================="
assert_contains "${status_all_output}" "==================== api ===================="
assert_contains "${status_all_output}" "==================== ui ===================="
assert_contains "${status_all_output}" "==================== subscriber ===================="

echo "checking broker api env var validation"
assert_command_fails_with \
  "start-broker-api.sh cannot continue because a required Solace environment variable is missing: SOLACE_CLOUD_HOST" \
  env -u SOLACE_CLOUD_HOST -u SOLACE_CLOUD_VPN -u SOLACE_CLOUD_USERNAME -u SOLACE_CLOUD_PASSWORD \
  "${REPO_ROOT}/scripts/start-broker-api.sh"

echo "checking subscriber env var validation"
assert_command_fails_with \
  "start-subscriber.sh cannot continue because a required Solace environment variable is missing: SOLACE_CLOUD_HOST" \
  env -u SOLACE_CLOUD_HOST -u SOLACE_CLOUD_VPN -u SOLACE_CLOUD_USERNAME -u SOLACE_CLOUD_PASSWORD \
  "${REPO_ROOT}/scripts/start-subscriber.sh"

echo "checking publisher ui node_modules validation"
temp_ui_dir="$(mktemp -d)"
trap 'rm -rf "${temp_ui_dir}"' EXIT
assert_command_fails_with \
  "node_modules is missing in ${temp_ui_dir}" \
  env PUBLISHER_UI_DIR="${temp_ui_dir}" \
  "${REPO_ROOT}/scripts/start-publisher-ui.sh"

echo "checking publisher ui build node_modules validation"
assert_command_fails_with \
  "node_modules is missing in ${temp_ui_dir}" \
  env PUBLISHER_UI_DIR="${temp_ui_dir}" \
  "${REPO_ROOT}/scripts/build-publisher-ui.sh"

echo "script smoke tests passed"
