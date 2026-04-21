#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "missing required command: ${command_name}" >&2
    exit 1
  fi
}

require_env_var() {
  local variable_name="$1"
  if [[ -z "${!variable_name:-}" ]]; then
    echo "missing required environment variable: ${variable_name}" >&2
    exit 1
  fi
}

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

enter_module() {
  local module_path="$1"
  if [[ "${module_path}" = /* ]]; then
    cd "${module_path}"
    return
  fi

  cd "${REPO_ROOT}/${module_path}"
}
