#!/usr/bin/env bash
#
# common.sh
#
# Purpose:
#   Provides shared utility functions for all scripts in the repository.
#
# Usage:
#   source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
#
# Required tools/dependencies:
#   - bash
#
# Expected output:
#   None (defines functions for other scripts).
#
# Exit behavior:
#   The utility functions may exit the script with code 1 if requirements are not met.

set -euo pipefail

# The root directory of the repository, calculated relative to this script's location.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Function: require_command
# Purpose: Checks if a command exists in the PATH.
# Inputs:
#   $1 - The name of the command to check.
# Outputs:
#   Prints an error message to stderr if the command is missing.
# Exit behavior:
#   Exits with code 1 if the command is not found.
require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "missing required command: ${command_name}" >&2
    exit 1
  fi
}

# Function: require_docker_compose
# Purpose: Requires Docker with the Compose plugin available.
require_docker_compose() {
  require_command docker
  if ! docker compose version >/dev/null 2>&1; then
    echo "missing required Docker Compose plugin: docker compose" >&2
    exit 1
  fi
}

# Function: java_major_version
# Purpose: Reads the major version from the configured Java runtime.
# Outputs:
#   The Java major version, such as 21.
java_major_version() {
  java -version 2>&1 \
    | awk -F '"' '/version/ {
        split($2, version_parts, ".")
        if (version_parts[1] == "1") {
          print version_parts[2]
        } else {
          print version_parts[1]
        }
        exit
      }'
}

# Function: require_java_major_version
# Purpose: Requires a specific Java major version before running Java modules.
# Inputs:
#   $1 - The expected Java major version.
# Exit behavior:
#   Exits with code 1 if Java is missing or if the active Java version differs.
require_java_major_version() {
  local expected_version="$1"
  require_command java

  local actual_version
  actual_version="$(java_major_version)"

  if [[ -z "${actual_version}" ]]; then
    echo "could not determine Java version; expected Java ${expected_version}" >&2
    exit 1
  fi

  if [[ "${actual_version}" != "${expected_version}" ]]; then
    echo "expected Java ${expected_version}, but found Java ${actual_version}" >&2
    echo "set JAVA_HOME/PATH to a Java ${expected_version} runtime and try again" >&2
    exit 1
  fi
}

# Function: separate_component_log_transitions
# Purpose: Inserts a blank line when a prefixed log stream changes component.
# Inputs:
#   Reads lines from stdin. Component lines must begin with "[component]".
# Outputs:
#   The original stream with one blank line before each component transition.
separate_component_log_transitions() {
  awk '
    {
      current_component = ""
      if ($0 ~ /^\[[^]]+\]/) {
        closing_bracket = index($0, "]")
        current_component = substr($0, 2, closing_bracket - 2)
      }

      if (previous_component != "" && current_component != "" && current_component != previous_component) {
        print ""
      }

      print $0
      fflush()

      if (current_component != "") {
        previous_component = current_component
      }
    }
  '
}

# Function: latest_start_all_log_dir
# Purpose: Reads the latest start-all log directory from the shared pointer file.
# Outputs:
#   The latest start-all log directory path, if it exists.
latest_start_all_log_dir() {
  local latest_log_dir_file="${LATEST_START_ALL_FILE:-${TMPDIR:-/tmp}/solace-start-all.latest}"

  if [[ ! -f "${latest_log_dir_file}" ]]; then
    return 0
  fi

  head -n 1 "${latest_log_dir_file}" 2>/dev/null | tr -d '\r' || true
}

# Function: workspace_stop_marker_path
# Purpose: Returns the external stop marker path for a start-all log directory.
# Inputs:
#   $1 - The start-all log directory.
# Outputs:
#   The stop marker file path.
workspace_stop_marker_path() {
  local log_dir="$1"
  printf '%s\n' "${log_dir}/stop-requested"
}

# Function: request_workspace_stop
# Purpose: Marks the latest start-all run as being intentionally stopped.
request_workspace_stop() {
  local log_dir
  log_dir="$(latest_start_all_log_dir)"

  if [[ -z "${log_dir}" || ! -d "${log_dir}" ]]; then
    return 1
  fi

  printf 'stop requested at %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" >"$(workspace_stop_marker_path "${log_dir}")"
  return 0
}

# Function: workspace_stop_requested
# Purpose: Checks whether a start-all log directory has an external stop marker.
# Inputs:
#   $1 - The start-all log directory.
workspace_stop_requested() {
  local log_dir="$1"
  [[ -f "$(workspace_stop_marker_path "${log_dir}")" ]]
}

# Function: is_windows_shell
# Purpose: Detects Windows-hosted Bash environments such as Git Bash, MSYS, or Cygwin.
# Outputs:
#   None.
# Exit behavior:
#   Returns 0 for Windows-hosted Bash, 1 otherwise.
is_windows_shell() {
  if [[ "${WORKSPACE_SCRIPT_PLATFORM:-}" == "windows" ]]; then
    return 0
  fi

  if [[ "${WORKSPACE_SCRIPT_PLATFORM:-}" == "unix" ]]; then
    return 1
  fi

  case "$(uname -s 2>/dev/null || echo unknown)" in
    MINGW*|MSYS*|CYGWIN*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

# Function: powershell_executable
# Purpose: Finds a PowerShell executable available from the current shell.
# Outputs:
#   The executable name/path to stdout.
# Exit behavior:
#   Returns 0 if PowerShell was found, 1 otherwise.
powershell_executable() {
  local candidate
  for candidate in pwsh.exe powershell.exe pwsh powershell; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      command -v "${candidate}"
      return 0
    fi
  done

  return 1
}

# Function: require_powershell
# Purpose: Requires PowerShell for Windows process discovery and shutdown.
# Outputs:
#   Prints an error message to stderr if PowerShell is missing.
# Exit behavior:
#   Exits with code 1 if PowerShell is unavailable.
require_powershell() {
  if ! powershell_executable >/dev/null 2>&1; then
    echo "missing required command: PowerShell (pwsh.exe or powershell.exe)" >&2
    exit 1
  fi
}

# Function: run_powershell
# Purpose: Runs a non-interactive PowerShell command.
# Inputs:
#   $1 - PowerShell command text.
# Outputs:
#   The PowerShell command output.
run_powershell() {
  local ps_bin
  ps_bin="$(powershell_executable)"
  "${ps_bin}" -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$1"
}

# Function: require_process_read_tools
# Purpose: Requires tools used for local process discovery.
require_process_read_tools() {
  if is_windows_shell; then
    require_powershell
    return
  fi

  require_command lsof
  require_command pgrep
  require_command ps
}

# Function: require_process_stop_tools
# Purpose: Requires tools used for local process shutdown.
require_process_stop_tools() {
  if is_windows_shell; then
    require_powershell
    return
  fi

  require_command kill
  require_command lsof
  require_command pgrep
}

# Function: listening_pid_for_port
# Purpose: Finds the PID of the process listening on a specific TCP port.
# Inputs:
#   $1 - The TCP port number.
# Outputs:
#   The PID to stdout, if found.
listening_pid_for_port() {
  local port="$1"

  if is_windows_shell; then
    WORKSPACE_PORT="${port}" run_powershell '
      $port = [int]$env:WORKSPACE_PORT
      Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -First 1
    ' 2>/dev/null | tr -d '\r' || true
    return
  fi

  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

# Function: listening_ports_for_pid
# Purpose: Finds all TCP ports a specific PID is listening on.
# Inputs:
#   $1 - The PID.
# Outputs:
#   List of port numbers to stdout.
listening_ports_for_pid() {
  local pid="$1"

  if is_windows_shell; then
    WORKSPACE_PID="${pid}" run_powershell '
      $processId = [int]$env:WORKSPACE_PID
      Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.OwningProcess -eq $processId } |
        Select-Object -ExpandProperty LocalPort |
        Sort-Object -Unique
    ' 2>/dev/null | tr -d '\r' || true
    return
  fi

  lsof -Pan -p "${pid}" -iTCP -sTCP:LISTEN 2>/dev/null \
    | awk 'NR > 1 { split($9, endpoint, ":"); print endpoint[length(endpoint)] }' \
    | sort -u
}

# Function: command_for_pid
# Purpose: Retrieves the full command line for a given PID.
# Inputs:
#   $1 - The PID.
# Outputs:
#   The command string to stdout.
command_for_pid() {
  local pid="$1"

  if is_windows_shell; then
    WORKSPACE_PID="${pid}" run_powershell '
      $processId = [int]$env:WORKSPACE_PID
      $process = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
      if ($null -ne $process) {
        if ([string]::IsNullOrWhiteSpace($process.CommandLine)) {
          $process.Name
        } else {
          $process.CommandLine
        }
      }
    ' 2>/dev/null | tr -d '\r' || true
    return
  fi

  ps -p "${pid}" -o command= 2>/dev/null | sed 's/^[[:space:]]*//' || true
}

# Function: find_matching_pids
# Purpose: Finds all PIDs matching a process command-line or process-name pattern.
# Inputs:
#   $1 - The pattern to match.
# Outputs:
#   List of matching PIDs to stdout.
find_matching_pids() {
  local pattern="$1"

  if is_windows_shell; then
    WORKSPACE_PROCESS_PATTERN="${pattern}" run_powershell '
      $pattern = $env:WORKSPACE_PROCESS_PATTERN
      Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
          ($_.CommandLine -like "*$pattern*") -or
          ($_.Name -like "*$pattern*")
        } |
        Select-Object -ExpandProperty ProcessId
    ' 2>/dev/null | tr -d '\r' || true
    return
  fi

  pgrep -f "${pattern}" 2>/dev/null || true
}

# Function: find_matching_pid
# Purpose: Finds the first PID matching a process command-line or process-name pattern.
# Inputs:
#   $1 - The pattern to match.
# Outputs:
#   The matching PID to stdout, if found.
find_matching_pid() {
  find_matching_pids "$1" | head -n 1
}

# Function: stop_pid_if_running
# Purpose: Sends a graceful termination request to a PID if it exists.
# Inputs:
#   $1 - The PID to stop.
# Exit behavior:
#   Returns 0 if the process was stopped, 1 otherwise.
stop_pid_if_running() {
  local pid="$1"

  if [[ -z "${pid}" ]]; then
    return 1
  fi

  if is_windows_shell; then
    WORKSPACE_PID="${pid}" run_powershell '
      $processId = [int]$env:WORKSPACE_PID
      $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
      if ($null -eq $process) {
        exit 1
      }
      Stop-Process -Id $processId -ErrorAction Stop
    ' >/dev/null 2>&1
    return $?
  fi

  if kill -0 "${pid}" 2>/dev/null; then
    kill "${pid}" 2>/dev/null || true
    return 0
  fi

  return 1
}

# Function: stop_process_tree_if_running
# Purpose: Sends a graceful termination request to a PID and its child processes.
# Inputs:
#   $1 - The root PID to stop.
# Notes:
#   This is best effort. It is mainly used to clean up helper shell pipelines
#   and wrapper scripts where child processes may otherwise continue writing.
stop_process_tree_if_running() {
  local pid="$1"

  if [[ -z "${pid}" ]]; then
    return 1
  fi

  if is_windows_shell; then
    WORKSPACE_PID="${pid}" run_powershell '
      $rootProcessId = [int]$env:WORKSPACE_PID
      $processesByParent = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Group-Object -Property ParentProcessId -AsHashTable -AsString

      function Stop-ProcessTree([int]$processId) {
        $key = [string]$processId
        if ($processesByParent.ContainsKey($key)) {
          foreach ($child in $processesByParent[$key]) {
            Stop-ProcessTree -processId ([int]$child.ProcessId)
          }
        }

        Stop-Process -Id $processId -ErrorAction SilentlyContinue
      }

      Stop-ProcessTree -processId $rootProcessId
    ' >/dev/null 2>&1 || true

    kill "${pid}" 2>/dev/null || true
    return 0
  fi

  if command -v pgrep >/dev/null 2>&1; then
    local child_pid
    while IFS= read -r child_pid; do
      if [[ -n "${child_pid}" ]]; then
        stop_process_tree_if_running "${child_pid}" || true
      fi
    done < <(pgrep -P "${pid}" 2>/dev/null || true)
  fi

  kill "${pid}" 2>/dev/null || true
  return 0
}

# Function: require_env_var
# Purpose: Checks if an environment variable is set and non-empty.
# Inputs:
#   $1 - The name of the environment variable to check.
# Outputs:
#   Prints an error message to stderr if the variable is missing.
# Exit behavior:
#   Exits with code 1 if the variable is not set or empty.
require_env_var() {
  local variable_name="$1"
  if [[ -z "${!variable_name:-}" ]]; then
    echo "missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
}

# Function: require_solace_env_vars
# Purpose: Validates that all required Solace Cloud environment variables are set.
# Inputs:
#   $1 - (Optional) Name of the caller script for logging purposes.
# Outputs:
#   Prints error messages to stderr listing the missing variables.
# Exit behavior:
#   Exits with code 1 if any required Solace variable is missing.
require_solace_env_vars() {
  local caller_name="${1:-this script}"
  local required_vars=(
    "SOLACE_CLOUD_HOST"
    "SOLACE_CLOUD_VPN"
    "SOLACE_CLOUD_USERNAME"
    "SOLACE_CLOUD_PASSWORD"
  )

  for variable_name in "${required_vars[@]}"; do
    if [[ -z "${!variable_name:-}" ]]; then
      {
        echo "${caller_name} cannot continue because a required Solace environment variable is missing: ${variable_name}"
        echo "required variables:"
        for required_var in "${required_vars[@]}"; do
          echo "- ${required_var}"
        done
      } >&2
      exit 1
    fi
  done
}

# Function: enter_module
# Purpose: Changes the current working directory to a specific module.
# Inputs:
#   $1 - The path to the module (absolute or relative to REPO_ROOT).
# Outputs:
#   None.
# Exit behavior:
#   Returns or exits if 'cd' fails (due to 'set -e').
enter_module() {
  local module_path="$1"
  if [[ "${module_path}" = /* ]]; then
    cd "${module_path}"
    return
  fi

  cd "${REPO_ROOT}/${module_path}"
}
