#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command node
require_command npm

enter_module "solace-publisher-ui"

if [[ ! -d node_modules ]]; then
  echo "node_modules is missing in solace-publisher-ui; run 'npm install' first" >&2
  exit 1
fi

echo "starting solace-publisher-ui on http://localhost:5173"
exec npm run dev
