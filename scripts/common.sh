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

enter_module() {
  local module_path="$1"
  if [[ "${module_path}" = /* ]]; then
    cd "${module_path}"
    return
  fi

  cd "${REPO_ROOT}/${module_path}"
}
