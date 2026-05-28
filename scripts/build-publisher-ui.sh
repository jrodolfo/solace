#!/usr/bin/env bash
#
# build-publisher-ui.sh
#
# Purpose:
#   Builds the solace-publisher-ui component using npm.
#   Installs dependencies if node_modules is missing.
#
# Usage:
#   ./build-publisher-ui.sh
#
# Required tools/dependencies:
#   - bash
#   - node
#   - npm
#
# Expected output:
#   npm install logs (if needed), npm build logs, and a production build in the dist folder.
#
# Exit behavior:
#   Exits with code 0 on success.
#   Exits with a non-zero code if dependencies installation or build fails.

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

echo "building solace-publisher-ui"
# Execute npm build.
exec npm run build
