#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

"${SCRIPT_DIR}/build-broker-api.sh"
"${SCRIPT_DIR}/build-publisher-ui.sh"
"${SCRIPT_DIR}/build-subscriber.sh"

echo "built solace-broker-api, solace-publisher-ui, and solace-subscriber"
