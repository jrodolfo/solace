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

start_process "api" "${SCRIPT_DIR}/start-broker-api.sh"
start_process "ui" "${SCRIPT_DIR}/start-publisher-ui.sh"
start_process "subscriber" "${SCRIPT_DIR}/start-subscriber.sh"

print_separator "workspace startup"
echo "started api, ui, and subscriber; watch the prefixed logs below"
echo "the ui local url appears in the [ui] vite output and may change if port 5173 is already in use"
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
  sleep 1
done
