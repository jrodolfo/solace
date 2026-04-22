#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command node
require_command npm

PUBLISHER_UI_DIR="${PUBLISHER_UI_DIR:-solace-publisher-ui}"

enter_module "${PUBLISHER_UI_DIR}"

if [[ ! -d node_modules ]]; then
  echo "node_modules is missing in ${PUBLISHER_UI_DIR}; run 'npm install' first" >&2
  exit 1
fi

echo "building solace-publisher-ui"
exec npm run build
