#!/usr/bin/env bash
#
# build-subscriber.sh
#
# Purpose:
#   Builds the solace-subscriber Java component using Maven.
#
# Usage:
#   ./build-subscriber.sh
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
require_java_major_version 21
require_command mvn

# Navigate to the component directory.
enter_module "solace-subscriber"

echo "building solace-subscriber"
# Execute maven build.
exec mvn clean package
