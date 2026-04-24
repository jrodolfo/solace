#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command curl
require_command lsof
require_command pgrep

API_PORT="${API_PORT:-8081}"
API_HEALTH_URL="${API_HEALTH_URL:-http://localhost:${API_PORT}/rest/actuator/health}"
SUBSCRIBER_JAR_PATTERN="${SUBSCRIBER_JAR_PATTERN:-solace-subscriber-1.0-SNAPSHOT-all.jar}"
UI_PROCESS_PATTERN="${UI_PROCESS_PATTERN:-vite}"
LATEST_LOG_DIR_FILE="${LATEST_START_ALL_FILE:-${TMPDIR:-/tmp}/solace-start-all.latest}"

print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

first_line_or_dash() {
  local value="${1:-}"
  if [[ -n "${value}" ]]; then
    printf '%s\n' "${value}" | head -n 1
  else
    echo "-"
  fi
}

listening_pid_for_port() {
  local port="$1"
  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

listening_ports_for_pid() {
  local pid="$1"
  lsof -Pan -p "${pid}" -iTCP -sTCP:LISTEN 2>/dev/null \
    | awk 'NR > 1 { split($9, endpoint, ":"); print endpoint[length(endpoint)] }' \
    | sort -u
}

command_for_pid() {
  local pid="$1"
  ps -p "${pid}" -o command= 2>/dev/null | sed 's/^[[:space:]]*//' || true
}

find_matching_pid() {
  local pattern="$1"
  pgrep -f "${pattern}" 2>/dev/null | head -n 1 || true
}

latest_start_all_log_dir() {
  if [[ ! -f "${LATEST_LOG_DIR_FILE}" ]]; then
    return
  fi

  local log_dir
  log_dir="$(head -n 1 "${LATEST_LOG_DIR_FILE}" 2>/dev/null | tr -d '\r' || true)"

  if [[ -n "${log_dir}" && -d "${log_dir}" ]]; then
    printf '%s\n' "${log_dir}"
  fi
}

ui_url_from_start_all_log() {
  local log_dir="$1"
  local ui_log_file="${log_dir}/ui.log"

  if [[ ! -f "${ui_log_file}" ]]; then
    return
  fi

  grep 'Local:' "${ui_log_file}" 2>/dev/null \
    | tail -n 1 \
    | sed -E 's/.*Local:[[:space:]]*(http:\/\/[^[:space:]]+).*/\1/' \
    | head -n 1
}

api_status() {
  print_separator "api"

  local pid
  pid="$(listening_pid_for_port "${API_PORT}")"
  local health_response=""
  local curl_exit_code=0

  set +e
  health_response="$(curl -fsS "${API_HEALTH_URL}" 2>/dev/null)"
  curl_exit_code=$?
  set -e

  if [[ ${curl_exit_code} -eq 0 ]]; then
    echo "status: RUNNING"
    echo "health: UP"
    echo "health url: ${API_HEALTH_URL}"
    echo "pid: ${pid:-unknown}"
    return
  fi

  if [[ -n "${pid}" ]]; then
    echo "status: RUNNING"
    echo "health: UNAVAILABLE"
    echo "health url: ${API_HEALTH_URL}"
    echo "pid: ${pid}"
    echo "note: port ${API_PORT} is listening, but the actuator health endpoint did not return successfully"
    return
  fi

  echo "status: NOT RUNNING"
  echo "health: UNAVAILABLE"
  echo "health url: ${API_HEALTH_URL}"
  echo "pid: -"
}

ui_status() {
  print_separator "ui"

  local latest_log_dir=""
  latest_log_dir="$(latest_start_all_log_dir)"
  local hinted_url=""
  if [[ -n "${latest_log_dir}" ]]; then
    hinted_url="$(ui_url_from_start_all_log "${latest_log_dir}")"
  fi

  local pid
  pid="$(find_matching_pid "${UI_PROCESS_PATTERN}")"

  if [[ -z "${pid}" ]]; then
    echo "status: NOT RUNNING"
    echo "pid: -"
    echo "url: -"
    if [[ -n "${hinted_url}" ]]; then
      echo "last known url: ${hinted_url}"
      echo "last known source: start-all ui log"
      echo "log dir: ${latest_log_dir}"
    fi
    return
  fi

  local command
  command="$(command_for_pid "${pid}")"
  mapfile -t ports < <(listening_ports_for_pid "${pid}")

  echo "status: RUNNING"
  echo "pid: ${pid}"
  echo "command: $(first_line_or_dash "${command}")"

  if [[ -n "${hinted_url}" ]]; then
    echo "url: ${hinted_url}"
    echo "source: last known start-all ui log"
    echo "log dir: ${latest_log_dir}"
    return
  fi

  if [[ ${#ports[@]} -eq 0 ]]; then
    echo "url: unknown"
    echo "note: a matching UI process is running, but no listening TCP port was detected"
    return
  fi

  echo "urls:"
  for port in "${ports[@]}"; do
    echo "- http://localhost:${port}"
  done
  echo "source: live port detection"
}

subscriber_status() {
  print_separator "subscriber"

  local pid
  pid="$(find_matching_pid "${SUBSCRIBER_JAR_PATTERN}")"

  if [[ -z "${pid}" ]]; then
    echo "status: NOT RUNNING"
    echo "pid: -"
    echo "command: -"
    return
  fi

  local command
  command="$(command_for_pid "${pid}")"

  echo "status: RUNNING"
  echo "pid: ${pid}"
  echo "command: $(first_line_or_dash "${command}")"
}

print_separator "workspace status"
echo "checking api, ui, and subscriber"

api_status
ui_status
subscriber_status
