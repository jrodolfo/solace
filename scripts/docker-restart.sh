#!/usr/bin/env bash
#
# docker-restart.sh
#
# Purpose:
#   Restarts the full Docker runtime.
#
# Usage:
#   ./scripts/docker-restart.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

"${SCRIPT_DIR}/docker-stop.sh"
"${SCRIPT_DIR}/docker-start.sh"
