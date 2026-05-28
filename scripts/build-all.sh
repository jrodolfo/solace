#!/usr/bin/env bash
#
# build-all.sh
#
# Purpose:
#   Orchestrates the build process for all components: solace-broker-api,
#   solace-publisher-ui, and solace-subscriber.
#
# Usage:
#   ./build-all.sh
#
# Required tools/dependencies:
#   - bash
#   - Tools required by individual build scripts (e.g., mvn, npm).
#
# Expected output:
#   Logs from individual build scripts and a final confirmation message.
#
# Exit behavior:
#   Exits with code 0 on success.
#   Exits with a non-zero code if any individual build fails.

set -euo pipefail

# Directory where this script is located.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Trigger builds for all components.
"${SCRIPT_DIR}/build-broker-api.sh"
"${SCRIPT_DIR}/build-publisher-ui.sh"
"${SCRIPT_DIR}/build-subscriber.sh"

echo "built solace-broker-api, solace-publisher-ui, and solace-subscriber"
