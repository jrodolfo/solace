#!/usr/bin/env bash
#
# restart-all.sh
#
# Purpose:
#   Restarts the entire workspace by stopping, rebuilding, and starting all components.
#
# Usage:
#   ./restart-all.sh
#
# Required tools/dependencies:
#   - bash
#   - Individual component build and run dependencies.
#
# Expected output:
#   Console output from stop, build, and start scripts with separators.
#
# Exit behavior:
#   Exits with code 0 on success.
#   Exits with a non-zero code if any sub-script fails.

set -euo pipefail

# Directory where this script is located.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Function: print_separator
# Purpose: Prints a formatted separator line with a label for better log readability.
# Inputs:
#   $1 - The label to display in the separator.
# Outputs:
#   A formatted string to stdout.
print_separator() {
  local label="$1"
  echo
  printf '==================== %s ====================\n' "${label}"
  echo
}

# Stop all running components.
print_separator "stopping workspace"
"${SCRIPT_DIR}/stop-all.sh"

# Rebuild all components.
print_separator "building workspace"
"${SCRIPT_DIR}/build-all.sh"

# Start all components.
print_separator "starting workspace"
"${SCRIPT_DIR}/start-all.sh"
