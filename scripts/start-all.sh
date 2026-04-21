#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS=()

cleanup() {
  if [[ "${#PIDS[@]}" -gt 0 ]]; then
    echo
    echo "stopping started processes"
    kill "${PIDS[@]}" 2>/dev/null || true
    wait "${PIDS[@]}" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

"${SCRIPT_DIR}/start-broker-api.sh" &
PIDS+=("$!")

"${SCRIPT_DIR}/start-publisher-ui.sh" &
PIDS+=("$!")

"${SCRIPT_DIR}/start-subscriber.sh" &
PIDS+=("$!")

echo "started api, ui, and subscriber; press ctrl-c to stop them"
wait -n "${PIDS[@]}"
