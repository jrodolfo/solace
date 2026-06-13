#!/usr/bin/env bash
#
# start-all.sh
#
# Purpose:
#   Starts all three components (API, UI, and Subscriber) in the background and
#   monitors their logs. It provides a unified view of the workspace logs and
#   announces readiness when components are healthy.
#   It also writes the latest combined-log directory to a temp pointer file so
#   status-all.sh can report the Vite URL selected during startup.
#
# Usage:
#   ./start-all.sh
#
# Required tools/dependencies:
#   - bash
#   - curl
#   - Solace environment variables (validated by common.sh).
#
# Expected output:
#   Prefixed logs from all three components and readiness messages with URLs.
#   Combined logs are written under "${TMPDIR:-/tmp}/solace-start-all.XXXXXX".
#   The latest generated log directory path is also written to
#   "${TMPDIR:-/tmp}/solace-start-all.latest" for status-all.sh.
#
# Exit behavior:
#   Runs indefinitely until interrupted (Ctrl+C) or one of the core processes exits.
#   Cleans up (stops) all background processes on exit.

set -euo pipefail

# Directory where this script is located.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# Source common utility functions.
source "${SCRIPT_DIR}/common.sh"

# State variables for process monitoring.
PIDS=()
PROCESS_NAMES=()
LOG_FILES=()
MONITOR_PIDS=()
PROCESS_STATUSES=()
SHUTTING_DOWN=0
# Create a temporary directory for component logs.
LOG_DIR="$(mktemp -d "${TMPDIR:-/tmp}/solace-start-all.XXXXXX")"
LOG_STREAM_FIFO="${LOG_DIR}/combined-log-stream.fifo"
LOG_FORMATTER_PID=""
LATEST_LOG_DIR_FILE="${LATEST_START_ALL_FILE:-${TMPDIR:-/tmp}/solace-start-all.latest}"
# Configuration for health checks.
API_BASE_URL="${API_BASE_URL:-http://localhost:8081}"
API_HEALTH_URL="${API_HEALTH_URL:-${API_BASE_URL}/rest/actuator/health}"
# Tracking for readiness announcements.
UI_URL_ANNOUNCED=0
API_URL_ANNOUNCED=0
WORKSPACE_URLS_ANNOUNCED=0

# Ensure required tools and environment variables are present.
require_command curl
require_solace_env_vars "start-all.sh"

# Function: print_separator
# Purpose: Prints a formatted separator line with a label.
# Inputs:
#   $1 - The label to display.
print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

# Function: prefix_log_stream
# Purpose: Adds a component prefix to each line of an input stream.
# Inputs:
#   $1 - The prefix to use (usually the component name).
prefix_log_stream() {
  local component_name="$1"
  awk -v prefix="[${component_name}] " '{ print prefix $0; fflush() }'
}

# Function: start_log_formatter
# Purpose: Starts the shared terminal formatter for all component log streams.
start_log_formatter() {
  mkfifo "${LOG_STREAM_FIFO}"
  exec 3<>"${LOG_STREAM_FIFO}"
  separate_component_log_transitions <"${LOG_STREAM_FIFO}" &
  LOG_FORMATTER_PID=$!
}

# Function: update_running_process_statuses
# Purpose: Updates the status of all currently running processes.
# Inputs:
#   $1 - The new status string.
update_running_process_statuses() {
  local replacement_status="$1"

  for i in "${!PROCESS_STATUSES[@]}"; do
    if [[ "${PROCESS_STATUSES[$i]}" == "running" ]]; then
      PROCESS_STATUSES[$i]="${replacement_status}"
    fi
  done
}

# Function: mark_running_processes_stopped_by_user
# Purpose: Marks all running processes as stopped by the user.
mark_running_processes_stopped_by_user() {
  for i in "${!PROCESS_STATUSES[@]}"; do
    if [[ "${PROCESS_STATUSES[$i]}" == "running" ]]; then
      PROCESS_STATUSES[$i]="stopped by user"
    fi
  done
}

# Function: find_log_file
# Purpose: Returns the path to the log file for a given component name.
# Inputs:
#   $1 - The component name.
# Outputs:
#   The log file path to stdout.
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

# Function: current_ui_local_url
# Purpose: Extracts the current Vite local URL from the UI component log.
# Outputs:
#   The detected URL to stdout, if found.
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

# Function: api_is_ready
# Purpose: Checks if the API is healthy by calling its health endpoint.
# Exit behavior:
#   Returns 0 if the API is healthy, non-zero otherwise.
api_is_ready() {
  curl -fsS "${API_HEALTH_URL}" >/dev/null 2>&1
}

# Function: announce_workspace_urls_if_ready
# Purpose: Prints API/UI URLs once readiness can be inferred from health/logs.
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

# Function: start_process
# Purpose: Starts a component script in the background and sets up log monitoring.
# Inputs:
#   $1 - Component name.
#   $2 - Path to the script to execute.
start_process() {
  local name="$1"
  local script_path="$2"
  local log_file="${LOG_DIR}/${name}.log"

  # Initialize/clear the log file.
  : >"${log_file}"

  print_separator "starting ${name}"
  echo "[${name}] streaming logs from ${log_file}"

  # Start the component script.
  "${script_path}" >"${log_file}" 2>&1 &
  local process_pid=$!

  # Start a monitor process to tail the logs with a prefix.
  (
    tail -n +1 -f "${log_file}" | prefix_log_stream "${name}" >"${LOG_STREAM_FIFO}"
  ) &
  local monitor_pid=$!

  # Record process metadata.
  PIDS+=("${process_pid}")
  PROCESS_NAMES+=("${name}")
  LOG_FILES+=("${log_file}")
  MONITOR_PIDS+=("${monitor_pid}")
  PROCESS_STATUSES+=("running")
}

# Function: stop_log_monitors
# Purpose: Kills the background log monitoring (tail) processes.
stop_log_monitors() {
  if [[ "${#MONITOR_PIDS[@]}" -eq 0 ]]; then
    return
  fi

  kill "${MONITOR_PIDS[@]}" 2>/dev/null || true
  wait "${MONITOR_PIDS[@]}" 2>/dev/null || true
}

# Function: stop_log_formatter
# Purpose: Stops the shared terminal log formatter.
stop_log_formatter() {
  if [[ -n "${LOG_FORMATTER_PID}" ]]; then
    kill "${LOG_FORMATTER_PID}" 2>/dev/null || true
    wait "${LOG_FORMATTER_PID}" 2>/dev/null || true
    LOG_FORMATTER_PID=""
  fi

  exec 3>&- 2>/dev/null || true
}

# Function: print_status_summary
# Purpose: Prints the final status of all managed processes.
print_status_summary() {
  print_separator "status summary"
  for i in "${!PROCESS_NAMES[@]}"; do
    echo "- ${PROCESS_NAMES[$i]}: ${PROCESS_STATUSES[$i]}"
  done
  echo "- combined logs: ${LOG_DIR}"
}

# Function: cleanup
# Purpose: Stops child components and log monitors on script exit.
# Side effects:
#   Leaves the combined log directory in place for later status inspection.
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
  stop_log_formatter
  print_status_summary
}

# Function: handle_signal
# Purpose: Handles termination signals (SIGINT, SIGTERM).
handle_signal() {
  mark_running_processes_stopped_by_user
  exit 130
}

# Set up exit and signal traps.
trap cleanup EXIT
trap handle_signal INT TERM

# Store the latest log directory for other scripts to reference.
printf '%s\n' "${LOG_DIR}" >"${LATEST_LOG_DIR_FILE}"

start_log_formatter

# Start all three components.
start_process "api" "${SCRIPT_DIR}/start-broker-api.sh"
start_process "ui" "${SCRIPT_DIR}/start-publisher-ui.sh"
start_process "subscriber" "${SCRIPT_DIR}/start-subscriber.sh"

print_separator "workspace startup"
echo "started api, ui, and subscriber; watch the prefixed logs below"
echo "a separate readiness block will print the api and ui urls once they are up"
echo "press ctrl-c to stop all three processes"

# Monitoring loop: check for process exits and readiness.
while true; do
  if workspace_stop_requested "${LOG_DIR}"; then
    update_running_process_statuses "stopped by stop-all.sh"

    echo
    echo "external stop request detected from stop-all.sh; stopping started processes"
    exit 0
  fi

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

      if workspace_stop_requested "${LOG_DIR}"; then
        PROCESS_STATUSES[$i]="stopped by stop-all.sh"
        update_running_process_statuses "stopped by stop-all.sh"

        echo
        echo "external stop request detected from stop-all.sh; stopping remaining processes"
        exit 0
      fi

      # The subscriber exiting is a warning but doesn't stop the whole stack.
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

      # For other processes, failure triggers a full shutdown.
      update_running_process_statuses "stopped after ${PROCESS_NAMES[$i]} exited"

      echo
      echo "${PROCESS_NAMES[$i]} exited with code ${exit_code}; stopping remaining processes"
      exit "${exit_code}"
    fi
  done
  announce_workspace_urls_if_ready
  sleep 1
done
