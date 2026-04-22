#!/usr/bin/env bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_command java
require_command mvn

enter_module "solace-subscriber"

echo "building solace-subscriber"
exec mvn clean package
