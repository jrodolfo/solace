#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command kill
require_command lsof
require_command pgrep

API_PORT="${API_PORT:-8081}"
SUBSCRIBER_JAR_PATTERN="${SUBSCRIBER_JAR_PATTERN:-solace-subscriber-1.0-SNAPSHOT-all.jar}"
UI_PROCESS_PATTERNS=(
  "solace-publisher-ui"
  "vite"
)

print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

listening_pid_for_port() {
  local port="$1"
  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

find_matching_pids() {
  local pattern="$1"
  pgrep -f "${pattern}" 2>/dev/null || true
}

kill_pid_if_running() {
  local pid="$1"
  if [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null; then
    kill "${pid}" 2>/dev/null || true
    return 0
  fi

  return 1
}

stop_api() {
  print_separator "api"

  local pid
  pid="$(listening_pid_for_port "${API_PORT}")"

  if [[ -z "${pid}" ]]; then
    echo "status: NOT RUNNING"
    echo "action: nothing to stop"
    return
  fi

  if kill_pid_if_running "${pid}"; then
    echo "status: STOPPED"
    echo "pid: ${pid}"
    echo "action: sent TERM to the process listening on port ${API_PORT}"
    return
  fi

  echo "status: UNKNOWN"
  echo "pid: ${pid}"
  echo "action: failed to stop the process cleanly"
}

stop_ui() {
  print_separator "ui"

  local candidate_pids=()
  local pid=""
  local pattern=""

  for pattern in "${UI_PROCESS_PATTERNS[@]}"; do
    while IFS= read -r pid; do
      if [[ -n "${pid}" ]]; then
        candidate_pids+=("${pid}")
      fi
    done < <(find_matching_pids "${pattern}")
  done

  if [[ "${#candidate_pids[@]}" -eq 0 ]]; then
    echo "status: NOT RUNNING"
    echo "action: nothing to stop"
    return
  fi

  local unique_pids=()
  local seen=" "
  for pid in "${candidate_pids[@]}"; do
    if [[ "${seen}" != *" ${pid} "* ]]; then
      unique_pids+=("${pid}")
      seen="${seen}${pid} "
    fi
  done

  local stopped_pids=()
  for pid in "${unique_pids[@]}"; do
    if kill_pid_if_running "${pid}"; then
      stopped_pids+=("${pid}")
    fi
  done

  if [[ "${#stopped_pids[@]}" -eq 0 ]]; then
    echo "status: UNKNOWN"
    echo "action: found matching UI processes, but none were stopped"
    return
  fi

  echo "status: STOPPED"
  echo "pids:"
  for pid in "${stopped_pids[@]}"; do
    echo "- ${pid}"
  done
  echo "action: sent TERM to matching UI process(es)"
}

stop_subscriber() {
  print_separator "subscriber"

  local subscriber_pids=()
  local pid=""
  while IFS= read -r pid; do
    if [[ -n "${pid}" ]]; then
      subscriber_pids+=("${pid}")
    fi
  done < <(find_matching_pids "${SUBSCRIBER_JAR_PATTERN}")

  if [[ "${#subscriber_pids[@]}" -eq 0 ]]; then
    echo "status: NOT RUNNING"
    echo "action: nothing to stop"
    return
  fi

  local stopped_pids=()
  for pid in "${subscriber_pids[@]}"; do
    if kill_pid_if_running "${pid}"; then
      stopped_pids+=("${pid}")
    fi
  done

  if [[ "${#stopped_pids[@]}" -eq 0 ]]; then
    echo "status: UNKNOWN"
    echo "action: found matching subscriber processes, but none were stopped"
    return
  fi

  echo "status: STOPPED"
  echo "pids:"
  for pid in "${stopped_pids[@]}"; do
    echo "- ${pid}"
  done
  echo "action: sent TERM to matching subscriber process(es)"
}

print_separator "workspace shutdown"
echo "stopping api, ui, and subscriber when they are running"

stop_api
stop_ui
stop_subscriber
