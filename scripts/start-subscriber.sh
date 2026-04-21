#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command java
require_command mvn
require_env_var SOLACE_CLOUD_HOST
require_env_var SOLACE_CLOUD_VPN
require_env_var SOLACE_CLOUD_USERNAME
require_env_var SOLACE_CLOUD_PASSWORD

enter_module "solace-subscriber"

echo "building solace-subscriber"
mvn package

echo "starting solace-subscriber"
exec java -jar target/solace-subscriber-1.0-SNAPSHOT.jar
