#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command java
require_command mvn
require_solace_env_vars "start-subscriber.sh"

enter_module "solace-subscriber"

echo "building solace-subscriber"
mvn package

echo "starting solace-subscriber"
exec java -jar target/solace-subscriber-1.0-SNAPSHOT-all.jar
