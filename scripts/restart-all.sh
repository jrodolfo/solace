#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

print_separator "stopping workspace"
"${SCRIPT_DIR}/stop-all.sh"

print_separator "building workspace"
"${SCRIPT_DIR}/build-all.sh"

print_separator "starting workspace"
"${SCRIPT_DIR}/start-all.sh"
