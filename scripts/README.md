# Root Scripts

This folder contains the repo-level helper scripts for the local workspace.

These scripts are wrappers around the three active modules:

- `solace-broker-api`
- `solace-publisher-ui`
- `solace-subscriber`

They are meant to give you a small, predictable operator workflow from the repo root.

## Build Scripts

- `build-broker-api.sh`
  Builds `solace-broker-api` with Maven.

- `build-publisher-ui.sh`
  Builds `solace-publisher-ui` with `npm run build`.
  This expects `solace-publisher-ui/node_modules` to already exist.

- `build-subscriber.sh`
  Builds `solace-subscriber` with Maven.

- `build-all.sh`
  Runs the three build scripts in sequence.

## Start Scripts

- `start-broker-api.sh`
  Starts `solace-broker-api` with `mvn spring-boot:run`.
  Requires the shared Solace environment variables.

- `start-publisher-ui.sh`
  Starts the Vite dev server for `solace-publisher-ui`.
  This expects `solace-publisher-ui/node_modules` to already exist.

- `start-subscriber.sh`
  Builds and starts `solace-subscriber`.
  Requires the shared Solace environment variables.

- `start-all.sh`
  Starts API, UI, and subscriber together.
  It validates the shared Solace environment variables first, streams prefixed logs, and writes a temporary combined-log directory that can also help `status-all.sh`.

## Stop / Restart / Status

- `stop-all.sh`
  Stops the locally detected API, UI, and subscriber processes when they are running.

- `restart-all.sh`
  Runs `stop-all.sh`, `build-all.sh`, and `start-all.sh` in sequence.

- `status-all.sh`
  Reports local workspace status for API, UI, and subscriber.

  Status model:
  - API:
    checked via `http://localhost:8081/rest/actuator/health`
  - UI:
    checked via matching Vite process plus detected listening ports, and when available the latest `start-all.sh` UI log is used to show the last known Vite `Local:` URL
  - subscriber:
    checked via matching Java process

## Shared Helper

- `common.sh`
  Shared helper functions used by the other scripts.
  This is not meant to be run directly.

## Script Smoke Test

- `test-scripts.sh`
  Runs lightweight smoke checks for the root scripts and their Makefile wiring.

## Recommended Entry Points

If you want the smallest useful set to remember:

- build everything:
  `./scripts/build-all.sh`
- start everything:
  `./scripts/start-all.sh`
- stop everything:
  `./scripts/stop-all.sh`
- restart everything:
  `./scripts/restart-all.sh`
- check status:
  `./scripts/status-all.sh`

The same workflows are also exposed through the root `Makefile`.
