#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command java
require_command mvn

enter_module "solace-broker-api"

echo "building solace-broker-api"
exec mvn clean package
