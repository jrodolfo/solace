#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

PIDS=()
PROCESS_NAMES=()
LOG_FILES=()
MONITOR_PIDS=()
PROCESS_STATUSES=()
SHUTTING_DOWN=0
LOG_DIR="$(mktemp -d "${TMPDIR:-/tmp}/solace-start-all.XXXXXX")"
LATEST_LOG_DIR_FILE="${LATEST_START_ALL_FILE:-${TMPDIR:-/tmp}/solace-start-all.latest}"
API_BASE_URL="${API_BASE_URL:-http://localhost:8081}"
API_HEALTH_URL="${API_HEALTH_URL:-${API_BASE_URL}/rest/actuator/health}"
UI_URL_ANNOUNCED=0
API_URL_ANNOUNCED=0
WORKSPACE_URLS_ANNOUNCED=0

require_command curl
require_solace_env_vars "start-all.sh"

print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

prefix_log_stream() {
  local component_name="$1"
  awk -v prefix="[${component_name}] " '{ print prefix $0; fflush() }'
}

update_running_process_statuses() {
  local replacement_status="$1"

  for i in "${!PROCESS_STATUSES[@]}"; do
    if [[ "${PROCESS_STATUSES[$i]}" == "running" ]]; then
      PROCESS_STATUSES[$i]="${replacement_status}"
    fi
  done
}

mark_running_processes_stopped_by_user() {
  for i in "${!PROCESS_STATUSES[@]}"; do
    if [[ "${PROCESS_STATUSES[$i]}" == "running" ]]; then
      PROCESS_STATUSES[$i]="stopped by user"
    fi
  done
}

find_log_file() {
  local target_name="$1"
  local i
  for i in "${!PROCESS_NAMES[@]}"; do
    if [[ "${PROCESS_NAMES[$i]}" == "${target_name}" ]]; then
      printf '%s\n' "${LOG_FILES[$i]}"
      return
    fi
  done
}

current_ui_local_url() {
  local ui_log_file=""
  ui_log_file="$(find_log_file "ui")"

  if [[ -z "${ui_log_file}" || ! -f "${ui_log_file}" ]]; then
    return 0
  fi

  grep 'Local:' "${ui_log_file}" 2>/dev/null \
    | tail -n 1 \
    | sed -E 's/.*Local:[[:space:]]*(http:\/\/[^[:space:]]+).*/\1/' \
    | head -n 1 || true
}

api_is_ready() {
  curl -fsS "${API_HEALTH_URL}" >/dev/null 2>&1
}

announce_workspace_urls_if_ready() {
  local ui_url=""
  ui_url="$(current_ui_local_url)"
  local api_ready=1

  if api_is_ready; then
    api_ready=0
  fi

  if [[ ${UI_URL_ANNOUNCED} -eq 0 && -n "${ui_url}" ]]; then
    print_separator "ui ready"
    echo "ui url: ${ui_url}"
    UI_URL_ANNOUNCED=1
  fi

  if [[ ${API_URL_ANNOUNCED} -eq 0 && ${api_ready} -eq 0 ]]; then
    print_separator "api ready"
    echo "api url: ${API_BASE_URL}"
    echo "health url: ${API_HEALTH_URL}"
    API_URL_ANNOUNCED=1
  fi

  if [[ ${WORKSPACE_URLS_ANNOUNCED} -eq 0 && ${api_ready} -eq 0 && -n "${ui_url}" ]]; then
    print_separator "workspace urls"
    echo "api: ${API_BASE_URL}"
    echo "ui: ${ui_url}"
    WORKSPACE_URLS_ANNOUNCED=1
  fi
}

start_process() {
  local name="$1"
  local script_path="$2"
  local log_file="${LOG_DIR}/${name}.log"

  : >"${log_file}"

  print_separator "starting ${name}"
  echo "[${name}] streaming logs from ${log_file}"

  "${script_path}" >"${log_file}" 2>&1 &
  local process_pid=$!

  (
    tail -n +1 -f "${log_file}" | prefix_log_stream "${name}"
  ) &
  local monitor_pid=$!

  PIDS+=("${process_pid}")
  PROCESS_NAMES+=("${name}")
  LOG_FILES+=("${log_file}")
  MONITOR_PIDS+=("${monitor_pid}")
  PROCESS_STATUSES+=("running")
}

stop_log_monitors() {
  if [[ "${#MONITOR_PIDS[@]}" -eq 0 ]]; then
    return
  fi

  kill "${MONITOR_PIDS[@]}" 2>/dev/null || true
  wait "${MONITOR_PIDS[@]}" 2>/dev/null || true
}

print_status_summary() {
  print_separator "status summary"
  for i in "${!PROCESS_NAMES[@]}"; do
    echo "- ${PROCESS_NAMES[$i]}: ${PROCESS_STATUSES[$i]}"
  done
  echo "- combined logs: ${LOG_DIR}"
}

cleanup() {
  if [[ "${SHUTTING_DOWN}" -eq 1 ]]; then
    return
  fi

  SHUTTING_DOWN=1

  if [[ "${#PIDS[@]}" -gt 0 ]]; then
    echo
    echo "stopping started processes"
    kill "${PIDS[@]}" 2>/dev/null || true
    wait "${PIDS[@]}" 2>/dev/null || true
  fi

  stop_log_monitors
  print_status_summary
}

handle_signal() {
  mark_running_processes_stopped_by_user
  exit 130
}

trap cleanup EXIT
trap handle_signal INT TERM

printf '%s\n' "${LOG_DIR}" >"${LATEST_LOG_DIR_FILE}"

start_process "api" "${SCRIPT_DIR}/start-broker-api.sh"
start_process "ui" "${SCRIPT_DIR}/start-publisher-ui.sh"
start_process "subscriber" "${SCRIPT_DIR}/start-subscriber.sh"

print_separator "workspace startup"
echo "started api, ui, and subscriber; watch the prefixed logs below"
echo "a separate readiness block will print the api and ui urls once they are up"
echo "press ctrl-c to stop all three processes"

while true; do
  for i in "${!PIDS[@]}"; do
    pid="${PIDS[$i]}"
    if [[ -z "${pid}" ]]; then
      continue
    fi

    if ! kill -0 "${pid}" 2>/dev/null; then
      if wait "${pid}"; then
        exit_code=0
      else
        exit_code=$?
      fi

      PROCESS_STATUSES[$i]="exited with code ${exit_code}"
      PIDS[$i]=""

      if [[ "${PROCESS_NAMES[$i]}" == "subscriber" ]]; then
        print_separator "subscriber warning"
        echo "subscriber exited with code ${exit_code}"
        echo
        echo "api and ui are still running intentionally"
        echo "fix the subscriber configuration if you want the full stack"
        echo "combined logs: ${LOG_DIR}"
        echo
        printf '============================================================\n'
        continue
      fi

      update_running_process_statuses "stopped after ${PROCESS_NAMES[$i]} exited"

      echo
      echo "${PROCESS_NAMES[$i]} exited with code ${exit_code}; stopping remaining processes"
      exit "${exit_code}"
    fi
  done
  announce_workspace_urls_if_ready
  sleep 1
done
