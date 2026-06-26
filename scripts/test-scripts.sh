#!/usr/bin/env bash
#
# test-scripts.sh
#
# Purpose:
#   Performs smoke tests and syntax checks on all scripts in the repository.
#   It validates 'make help' output, script syntax, and specific behaviors like
#   environment variable validation and UI auto-installation.
#
# Usage:
#   ./test-scripts.sh
#
# Required tools/dependencies:
#   - bash
#   - make
#   - Tools used by scripts (mktemp, etc.).
#
# Expected output:
#   Progress messages for each test and a final "passed" message.
#
# Exit behavior:
#   Exits with code 0 if all tests pass.
#   Exits with code 1 if any assertion or test fails.

set -euo pipefail

# Directory and repository root discovery.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Temporary directories and files for testing.
temp_status_dir=""
temp_stop_dir=""
temp_log_multiplexer_dir=""
temp_ui_dir=""
temp_process_bin_dir=""
temp_java_bin_dir=""
temp_fake_bin_dir=""
temp_npm_log_file=""

# Function: cleanup
# Purpose: Removes temporary directories and files created during testing.
cleanup() {
  if [[ -n "${temp_status_dir}" ]]; then
    rm -rf "${temp_status_dir}"
  fi

  if [[ -n "${temp_stop_dir}" ]]; then
    rm -rf "${temp_stop_dir}"
  fi

  if [[ -n "${temp_log_multiplexer_dir}" ]]; then
    rm -rf "${temp_log_multiplexer_dir}"
  fi

  if [[ -n "${temp_ui_dir}" ]]; then
    rm -rf "${temp_ui_dir}"
  fi

  if [[ -n "${temp_process_bin_dir}" ]]; then
    rm -rf "${temp_process_bin_dir}"
  fi

  if [[ -n "${temp_java_bin_dir}" ]]; then
    rm -rf "${temp_java_bin_dir}"
  fi

  if [[ -n "${temp_fake_bin_dir}" ]]; then
    rm -rf "${temp_fake_bin_dir}"
  fi
}

# Register cleanup on exit.
trap cleanup EXIT

# Function: assert_contains
# Purpose: Verifies that a string contains a specific substring.
# Inputs:
#   $1 - The string to search (haystack).
#   $2 - The substring to look for (needle).
# Exit behavior:
#   Exits with code 1 if the needle is not found.
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

# Function: assert_equals
# Purpose: Verifies that two strings are identical.
# Inputs:
#   $1 - Expected string.
#   $2 - Actual string.
# Exit behavior:
#   Exits with code 1 if the strings differ.
assert_equals() {
  local expected="$1"
  local actual="$2"

  if [[ "${actual}" != "${expected}" ]]; then
    echo "expected output:" >&2
    echo "${expected}" >&2
    echo "actual output:" >&2
    echo "${actual}" >&2
    exit 1
  fi
}

# Function: assert_command_fails_with
# Purpose: Verifies that a command fails and its output contains expected text.
# Inputs:
#   $1 - Expected error message.
#   $@ - The command and arguments to execute.
# Exit behavior:
#   Exits with code 1 if the command succeeds or error message is missing.
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

# --- Tests ---

echo "checking make help output"
make_help_output="$(make -C "${REPO_ROOT}" help)"
assert_contains "${make_help_output}" "make build-api"
assert_contains "${make_help_output}" "make build-all"
assert_contains "${make_help_output}" "make start-api"
assert_contains "${make_help_output}" "make stop-all"
assert_contains "${make_help_output}" "make restart-all"
assert_contains "${make_help_output}" "make status-all"
assert_contains "${make_help_output}" "make docker-build-all"
assert_contains "${make_help_output}" "make docker-start"
assert_contains "${make_help_output}" "make docker-stop"
assert_contains "${make_help_output}" "make docker-status"
assert_contains "${make_help_output}" "make docker-restart"
assert_contains "${make_help_output}" "make docker-logs"
assert_contains "${make_help_output}" "make docker-scan"
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
  "${REPO_ROOT}/scripts/docker-build-all.sh" \
  "${REPO_ROOT}/scripts/docker-start.sh" \
  "${REPO_ROOT}/scripts/docker-stop.sh" \
  "${REPO_ROOT}/scripts/docker-status.sh" \
  "${REPO_ROOT}/scripts/docker-restart.sh" \
  "${REPO_ROOT}/scripts/docker-logs.sh" \
  "${REPO_ROOT}/scripts/docker-scan.sh" \
  "${REPO_ROOT}/scripts/build-broker-api.sh" \
  "${REPO_ROOT}/scripts/build-publisher-ui.sh" \
  "${REPO_ROOT}/scripts/build-subscriber.sh" \
  "${REPO_ROOT}/scripts/build-all.sh"

echo "checking combined log component separation"
# shellcheck source=common.sh
source "${REPO_ROOT}/scripts/common.sh"
combined_log_sample="$(
  printf '%s\n' \
    "[api] api line 1" \
    "[api] api line 2" \
    "[subscriber] subscriber line 1" \
    "[subscriber] subscriber line 2" \
    "[ui] ui line 1" \
    "[api] api line 3" \
    | separate_component_log_transitions
)"
expected_combined_log_sample="$(cat <<'EOF'
[api] api line 1
[api] api line 2

[subscriber] subscriber line 1
[subscriber] subscriber line 2

[ui] ui line 1

[api] api line 3
EOF
)"
assert_equals "${expected_combined_log_sample}" "${combined_log_sample}"

echo "checking Java version parser"
temp_java_bin_dir="$(mktemp -d)"
cat >"${temp_java_bin_dir}/java" <<'EOF'
#!/usr/bin/env bash
echo 'openjdk version "21.0.7" 2026-04-15' >&2
EOF
chmod +x "${temp_java_bin_dir}/java"
java_21_version="$(PATH="${temp_java_bin_dir}:$PATH" java_major_version)"
assert_equals "21" "${java_21_version}"
cat >"${temp_java_bin_dir}/java" <<'EOF'
#!/usr/bin/env bash
echo 'java version "1.8.0_452"' >&2
EOF
chmod +x "${temp_java_bin_dir}/java"
java_8_version="$(PATH="${temp_java_bin_dir}:$PATH" java_major_version)"
assert_equals "8" "${java_8_version}"

echo "checking start-all log multiplexer handles initially empty logs"
temp_log_multiplexer_dir="$(mktemp -d)"
multiplexer_output_file="${temp_log_multiplexer_dir}/output.log"

(
  export SOLACE_CLOUD_HOST="test-host"
  export SOLACE_CLOUD_VPN="test-vpn"
  export SOLACE_CLOUD_USERNAME="test-user"
  export SOLACE_CLOUD_PASSWORD="test-password"
  export START_ALL_LIB_ONLY=1
  # shellcheck source=start-all.sh
  source "${REPO_ROOT}/scripts/start-all.sh"

  LOG_FILES=("${temp_log_multiplexer_dir}/api.log" "${temp_log_multiplexer_dir}/subscriber.log")
  PROCESS_NAMES=("api" "subscriber")
  LOG_MULTIPLEXER_PID=""
  LOG_STREAMING_STOPPED=0
  : >"${LOG_FILES[0]}"
  : >"${LOG_FILES[1]}"

  start_log_multiplexer
  sleep 0.4
  printf '%s\n' "api line after startup" >>"${LOG_FILES[0]}"
  sleep 0.4
  printf '%s\n' "subscriber line after startup" >>"${LOG_FILES[1]}"
  sleep 0.6
  stop_log_multiplexer
) >"${multiplexer_output_file}"
multiplexer_output="$(cat "${multiplexer_output_file}")"
assert_contains "${multiplexer_output}" "[api] api line after startup"
assert_contains "${multiplexer_output}" "[subscriber] subscriber line after startup"

echo "checking external workspace stop marker handling"
temp_stop_dir="$(mktemp -d)"
temp_stop_pointer_file="${temp_stop_dir}/latest-start-all.txt"
mkdir -p "${temp_stop_dir}/logs"
printf '%s\n' "${temp_stop_dir}/logs" >"${temp_stop_pointer_file}"
LATEST_START_ALL_FILE="${temp_stop_pointer_file}" request_workspace_stop
if [[ ! -f "${temp_stop_dir}/logs/stop-requested" ]]; then
  echo "expected stop marker to be created" >&2
  exit 1
fi
if ! workspace_stop_requested "${temp_stop_dir}/logs"; then
  echo "expected stop marker to be detected" >&2
  exit 1
fi
rm -f "${temp_stop_dir}/logs/stop-requested"
if workspace_stop_requested "${temp_stop_dir}/logs"; then
  echo "did not expect stop marker after removal" >&2
  exit 1
fi

echo "checking legacy package namespace is absent"
legacy_dot_pattern="org[.]orgname"
legacy_path_pattern="org""/""orgname"
set +e
legacy_namespace_matches="$(
  grep -R -n -E "${legacy_dot_pattern}|${legacy_path_pattern}" "${REPO_ROOT}" \
    --exclude-dir=.git \
    --exclude-dir=.idea \
    --exclude-dir=target \
    --exclude-dir=node_modules \
    --exclude-dir=dist \
    --exclude-dir=mysql-data \
    --exclude='*.class' \
    --exclude='*.jar' \
    --exclude='*.war' 2>/dev/null
)"
legacy_namespace_exit_code=$?
set -e
if [[ ${legacy_namespace_exit_code} -eq 0 ]]; then
  echo "legacy package namespace references found:" >&2
  echo "${legacy_namespace_matches}" >&2
  exit 1
fi
if [[ ${legacy_namespace_exit_code} -ne 1 ]]; then
  echo "legacy package namespace check failed" >&2
  exit 1
fi

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
stop_all_output="$(env STOP_ALL_MARKER_GRACE_SECONDS=0 "${REPO_ROOT}/scripts/stop-all.sh")"
assert_contains "${stop_all_output}" "==================== workspace shutdown ===================="
assert_contains "${stop_all_output}" "==================== api ===================="
assert_contains "${stop_all_output}" "==================== ui ===================="
assert_contains "${stop_all_output}" "==================== subscriber ===================="

echo "checking Windows Git Bash process helper fallback"
temp_process_bin_dir="$(mktemp -d)"
cat >"${temp_process_bin_dir}/pwsh.exe" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "${temp_process_bin_dir}/pwsh.exe"
windows_status_output="$(
  env PATH="${temp_process_bin_dir}:$PATH" WORKSPACE_SCRIPT_PLATFORM=windows \
    "${REPO_ROOT}/scripts/status-all.sh"
)"
assert_contains "${windows_status_output}" "==================== workspace status ===================="
assert_contains "${windows_status_output}" "status: NOT RUNNING"
windows_stop_output="$(
  env PATH="${temp_process_bin_dir}:$PATH" WORKSPACE_SCRIPT_PLATFORM=windows STOP_ALL_MARKER_GRACE_SECONDS=0 \
    "${REPO_ROOT}/scripts/stop-all.sh"
)"
assert_contains "${windows_stop_output}" "==================== workspace shutdown ===================="
assert_contains "${windows_stop_output}" "action: nothing to stop"

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

echo "checking docker log component validation"
temp_fake_bin_dir="$(mktemp -d)"
cat >"${temp_fake_bin_dir}/docker" <<'EOF'
#!/usr/bin/env bash
if [[ "$1" == "compose" && "$2" == "version" ]]; then
  exit 0
fi
if [[ "$1" == "compose" && "$2" == "logs" ]]; then
  printf '%s\n' "$*"
  exit 0
fi
echo "unexpected docker invocation: $*" >&2
exit 1
EOF
chmod +x "${temp_fake_bin_dir}/docker"
docker_subscriber_logs_output="$(PATH="${temp_fake_bin_dir}:$PATH" "${REPO_ROOT}/scripts/docker-logs.sh" subscriber)"
assert_contains "${docker_subscriber_logs_output}" "compose logs -f solace-subscriber"
assert_command_fails_with \
  "unknown component: wrong-component" \
  env PATH="${temp_fake_bin_dir}:$PATH" \
  "${REPO_ROOT}/scripts/docker-logs.sh" wrong-component

echo "checking docker build all behavior"
temp_fake_bin_dir="$(mktemp -d)"
docker_build_log_file="${temp_fake_bin_dir}/docker.log"
cat >"${temp_fake_bin_dir}/docker" <<EOF
#!/usr/bin/env bash
echo "\$*" >>"${docker_build_log_file}"
if [[ "\$1" == "compose" && "\$2" == "version" ]]; then
  exit 0
fi
if [[ "\$1" == "compose" && "\$2" == "pull" && "\$3" == "mysql" ]]; then
  exit 0
fi
if [[ "\$1" == "compose" && "\$2" == "build" ]]; then
  exit 0
fi
echo "unexpected docker invocation: \$*" >&2
exit 1
EOF
chmod +x "${temp_fake_bin_dir}/docker"
docker_build_output="$(PATH="${temp_fake_bin_dir}:$PATH" "${REPO_ROOT}/scripts/docker-build-all.sh")"
assert_contains "${docker_build_output}" "Docker runtime images are ready"
docker_build_log="$(cat "${docker_build_log_file}")"
assert_contains "${docker_build_log}" "compose pull mysql"
assert_contains "${docker_build_log}" "compose build solace-broker-api solace-subscriber"
assert_contains "${docker_build_log}" "compose build --no-cache solace-publisher-ui"

echo "checking docker image scan behavior"
temp_fake_bin_dir="$(mktemp -d)"
trivy_log_file="${temp_fake_bin_dir}/trivy.log"
cat >"${temp_fake_bin_dir}/docker" <<'EOF'
#!/usr/bin/env bash
if [[ "$1" == "compose" && "$2" == "version" ]]; then
  exit 0
fi
if [[ "$1" == "image" && "$2" == "inspect" ]]; then
  exit 0
fi
echo "unexpected docker invocation: $*" >&2
exit 1
EOF
cat >"${temp_fake_bin_dir}/trivy" <<EOF
#!/usr/bin/env bash
echo "\$*" >>"${trivy_log_file}"
exit 0
EOF
chmod +x "${temp_fake_bin_dir}/docker" "${temp_fake_bin_dir}/trivy"
docker_scan_output="$(PATH="${temp_fake_bin_dir}:$PATH" "${REPO_ROOT}/scripts/docker-scan.sh")"
assert_contains "${docker_scan_output}" "mode: release"
assert_contains "${docker_scan_output}" "docker image security scan passed"
docker_scan_trivy_log="$(cat "${trivy_log_file}")"
assert_contains "${docker_scan_trivy_log}" "--severity HIGH,CRITICAL --exit-code 1 --ignore-unfixed mysql:8.4"
assert_contains "${docker_scan_trivy_log}" "solace-broker-api:local"
assert_contains "${docker_scan_trivy_log}" "solace-publisher-ui:local"
assert_contains "${docker_scan_trivy_log}" "solace-subscriber:local"
: >"${trivy_log_file}"
docker_scan_full_output="$(PATH="${temp_fake_bin_dir}:$PATH" "${REPO_ROOT}/scripts/docker-scan.sh" --full)"
assert_contains "${docker_scan_full_output}" "mode: full"
docker_scan_full_trivy_log="$(cat "${trivy_log_file}")"
assert_contains "${docker_scan_full_trivy_log}" "--severity LOW,MEDIUM,HIGH,CRITICAL --exit-code 0 mysql:8.4"
assert_command_fails_with \
  "unknown option: --bad-option" \
  env PATH="${temp_fake_bin_dir}:$PATH" \
  "${REPO_ROOT}/scripts/docker-scan.sh" --bad-option

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
