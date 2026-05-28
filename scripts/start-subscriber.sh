#!/usr/bin/env bash
#
# start-subscriber.sh
#
# Purpose:
#   Builds and starts the solace-subscriber component.
#
# Usage:
#   ./start-subscriber.sh
#
# Required tools/dependencies:
#   - bash
#   - java
#   - mvn
#   - Solace environment variables (validated by common.sh).
#
# Expected output:
#   Maven build logs followed by the subscriber application logs.
#
# Exit behavior:
#   Exits with the exit code of the 'java -jar' command.

set -euo pipefail

# Source common utility functions.
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

# Ensure required commands and environment variables are available.
require_command java
require_command mvn
require_solace_env_vars "start-subscriber.sh"

# Navigate to the component directory.
enter_module "solace-subscriber"

echo "building solace-subscriber"
# Package the application.
mvn package

echo "starting solace-subscriber"
# Execute the shaded jar.
exec java -jar target/solace-subscriber-1.0-SNAPSHOT-all.jar
