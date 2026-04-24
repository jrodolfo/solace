#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
temp_status_dir=""
temp_ui_dir=""
temp_fake_bin_dir=""
temp_npm_log_file=""

cleanup() {
  if [[ -n "${temp_status_dir}" ]]; then
    rm -rf "${temp_status_dir}"
  fi

  if [[ -n "${temp_ui_dir}" ]]; then
    rm -rf "${temp_ui_dir}"
  fi

  if [[ -n "${temp_fake_bin_dir}" ]]; then
    rm -rf "${temp_fake_bin_dir}"
  fi
}

trap cleanup EXIT

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
assert_contains "${make_help_output}" "make stop-all"
assert_contains "${make_help_output}" "make restart-all"
assert_contains "${make_help_output}" "make status-all"
assert_contains "${make_help_output}" "make test-scripts"

echo "checking shell script syntax"
bash -n \
  "${REPO_ROOT}/scripts/common.sh" \
  "${REPO_ROOT}/scripts/start-broker-api.sh" \
  "${REPO_ROOT}/scripts/start-publisher-ui.sh" \
  "${REPO_ROOT}/scripts/start-subscriber.sh" \
  "${REPO_ROOT}/scripts/start-all.sh" \
  "${REPO_ROOT}/scripts/stop-all.sh" \
  "${REPO_ROOT}/scripts/restart-all.sh" \
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

echo "checking status-all ui log hint handling"
temp_status_dir="$(mktemp -d)"
temp_status_pointer_file="${temp_status_dir}/latest-start-all.txt"
mkdir -p "${temp_status_dir}/logs"
cat >"${temp_status_dir}/logs/ui.log" <<'EOF'

  VITE v6.0.11  ready in 63 ms

  ➜  Local:   http://localhost:5174/
EOF
printf '%s\n' "${temp_status_dir}/logs" >"${temp_status_pointer_file}"
status_all_with_hint_output="$(env LATEST_START_ALL_FILE="${temp_status_pointer_file}" "${REPO_ROOT}/scripts/status-all.sh")"
assert_contains "${status_all_with_hint_output}" "last known url: http://localhost:5174/"
assert_contains "${status_all_with_hint_output}" "last known source: start-all ui log"

echo "checking stop-all output structure"
stop_all_output="$("${REPO_ROOT}/scripts/stop-all.sh")"
assert_contains "${stop_all_output}" "==================== workspace shutdown ===================="
assert_contains "${stop_all_output}" "==================== api ===================="
assert_contains "${stop_all_output}" "==================== ui ===================="
assert_contains "${stop_all_output}" "==================== subscriber ===================="

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

echo "checking publisher ui auto-install behavior"
temp_ui_dir="$(mktemp -d)"
temp_fake_bin_dir="$(mktemp -d)"
temp_npm_log_file="${temp_fake_bin_dir}/npm.log"
cat >"${temp_fake_bin_dir}/npm" <<EOF
#!/usr/bin/env bash
echo "\$*" >>"${temp_npm_log_file}"
if [[ "\$1" == "install" ]]; then
  mkdir -p node_modules
  exit 0
fi
if [[ "\$1" == "run" && "\$2" == "dev" ]]; then
  exit 0
fi
echo "unexpected npm invocation: \$*" >&2
exit 1
EOF
chmod +x "${temp_fake_bin_dir}/npm"
env PATH="${temp_fake_bin_dir}:$PATH" PUBLISHER_UI_DIR="${temp_ui_dir}" \
  "${REPO_ROOT}/scripts/start-publisher-ui.sh" >/dev/null 2>&1
publisher_ui_start_npm_log="$(cat "${temp_npm_log_file}")"
assert_contains "${publisher_ui_start_npm_log}" "install"
assert_contains "${publisher_ui_start_npm_log}" "run dev"

echo "checking publisher ui build auto-install behavior"
rm -rf "${temp_ui_dir}/node_modules"
: >"${temp_npm_log_file}"
cat >"${temp_fake_bin_dir}/npm" <<EOF
#!/usr/bin/env bash
echo "\$*" >>"${temp_npm_log_file}"
if [[ "\$1" == "install" ]]; then
  mkdir -p node_modules
  exit 0
fi
if [[ "\$1" == "run" && "\$2" == "build" ]]; then
  exit 0
fi
echo "unexpected npm invocation: \$*" >&2
exit 1
EOF
chmod +x "${temp_fake_bin_dir}/npm"
env PATH="${temp_fake_bin_dir}:$PATH" PUBLISHER_UI_DIR="${temp_ui_dir}" \
  "${REPO_ROOT}/scripts/build-publisher-ui.sh" >/dev/null 2>&1
publisher_ui_build_npm_log="$(cat "${temp_npm_log_file}")"
assert_contains "${publisher_ui_build_npm_log}" "install"
assert_contains "${publisher_ui_build_npm_log}" "run build"

echo "script smoke tests passed"
