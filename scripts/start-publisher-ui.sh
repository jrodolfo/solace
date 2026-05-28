#!/usr/bin/env bash
#
# start-publisher-ui.sh
#
# Purpose:
#   Starts the solace-publisher-ui component in development mode using Vite.
#   Installs dependencies if node_modules is missing.
#
# Usage:
#   ./start-publisher-ui.sh
#
# Required tools/dependencies:
#   - bash
#   - node
#   - npm
#
# Expected output:
#   npm install logs (if needed) and Vite development server logs.
#
# Exit behavior:
#   Exits with the exit code of the 'npm run dev' command.

set -euo pipefail

# Source common utility functions.
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

# Ensure required commands are available.
require_command node
require_command npm

# Use environment variable for module directory or default to solace-publisher-ui.
PUBLISHER_UI_DIR="${PUBLISHER_UI_DIR:-solace-publisher-ui}"

# Navigate to the component directory.
enter_module "${PUBLISHER_UI_DIR}"

# Check for dependencies and install them if they are missing.
if [[ ! -d node_modules ]]; then
  echo "node_modules is missing in ${PUBLISHER_UI_DIR}; running 'npm install' first"
  npm install
fi

echo "starting solace-publisher-ui with vite"
# Start the Vite development server.
exec npm run dev
