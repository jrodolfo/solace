#!/usr/bin/env bash
#
# start-broker-api.sh
#
# Purpose:
#   Starts the solace-broker-api component using Maven and Spring Boot.
#
# Usage:
#   ./start-broker-api.sh
#
# Required tools/dependencies:
#   - bash
#   - java
#   - mvn
#   - Solace environment variables (validated by common.sh).
#
# Expected output:
#   Spring Boot application logs.
#
# Exit behavior:
#   Exits with the exit code of the 'mvn spring-boot:run' command.

set -euo pipefail

# Source common utility functions.
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

# Ensure required commands and environment variables are available.
require_command mvn
require_solace_env_vars "start-broker-api.sh"
require_java_major_version 21

# Navigate to the component directory.
enter_module "solace-broker-api"

echo "starting solace-broker-api on http://localhost:8081"
# Start the Spring Boot application.
exec mvn spring-boot:run
