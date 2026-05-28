#!/usr/bin/env bash
#
# build-broker-api.sh
#
# Purpose:
#   Builds the solace-broker-api Java component using Maven.
#
# Usage:
#   ./build-broker-api.sh
#
# Required tools/dependencies:
#   - bash
#   - java
#   - mvn
#
# Expected output:
#   Maven build logs and a final package in the target directory.
#
# Exit behavior:
#   Exits with code 0 on success.
#   Exits with a non-zero code if the Maven build fails.

set -euo pipefail

# Source common utility functions.
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

# Ensure required commands are available.
require_command java
require_command mvn

# Navigate to the component directory.
enter_module "solace-broker-api"

echo "building solace-broker-api"
# Execute maven build.
exec mvn clean package
