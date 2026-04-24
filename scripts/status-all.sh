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

  local pid
  pid="$(find_matching_pid "${UI_PROCESS_PATTERN}")"

  if [[ -z "${pid}" ]]; then
    echo "status: NOT RUNNING"
    echo "pid: -"
    echo "url: -"
    return
  fi

  local command
  command="$(command_for_pid "${pid}")"
  mapfile -t ports < <(listening_ports_for_pid "${pid}")

  echo "status: RUNNING"
  echo "pid: ${pid}"
  echo "command: $(first_line_or_dash "${command}")"

  if [[ ${#ports[@]} -eq 0 ]]; then
    echo "url: unknown"
    echo "note: a matching UI process is running, but no listening TCP port was detected"
    return
  fi

  echo "urls:"
  for port in "${ports[@]}"; do
    echo "- http://localhost:${port}"
  done
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
