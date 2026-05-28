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
