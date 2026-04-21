#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command java
require_command mvn
require_solace_env_vars "start-broker-api.sh"

enter_module "solace-broker-api"

echo "starting solace-broker-api on http://localhost:8081"
exec mvn spring-boot:run
