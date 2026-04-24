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
  If `node_modules` is missing, it runs `npm install` first.

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
  If `node_modules` is missing, it runs `npm install` first.

- `start-subscriber.sh`
  Builds and starts `solace-subscriber`.
  Requires the shared Solace environment variables.

- `start-all.sh`
  Starts API, UI, and subscriber together.
  It validates the shared Solace environment variables first, streams prefixed logs, and writes a temporary combined-log directory that can also help `status-all.sh`.
  Once the API and UI are ready, it also prints a clearer readiness block with their URLs.

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

## Detailed Notes

- `build-broker-api.sh` runs `mvn clean package` inside `solace-broker-api`.
- `build-subscriber.sh` runs `mvn clean package` inside `solace-subscriber`.
- `build-publisher-ui.sh` runs `npm run build` inside `solace-publisher-ui` and runs `npm install` first when `node_modules` is missing.
- `build-all.sh` runs the three module builds sequentially from the repo root.
- `start-broker-api.sh` and `start-subscriber.sh` require the shared Solace environment variables.
- `start-publisher-ui.sh` starts the Vite dev server and runs `npm install` first when `solace-publisher-ui/node_modules` is missing.
- `start-all.sh` validates the shared Solace environment variables before starting any child processes, streams prefixed `[api]`, `[ui]`, and `[subscriber]` logs from a temporary combined-log directory, prints a clear readiness block with the API and UI URLs once they are up, and prints a status summary when it stops.
- `stop-all.sh` sends `TERM` to the locally detected API, UI, and subscriber processes when they are running and reports which components were stopped versus already down.
- `restart-all.sh` runs `stop-all.sh`, `build-all.sh`, and `start-all.sh` in that order with clear step separators and fails fast if any step fails.
- `status-all.sh` reports local status for the three components using a hybrid model:
  - API via `http://localhost:8081/rest/actuator/health`
  - UI via the last known `start-all.sh` Vite `Local:` URL when available, otherwise matching Vite process plus detected listening port(s)
  - subscriber via matching Java process
- the Vite UI port is dynamic when `5173` is busy; `status-all.sh` reuses the latest `start-all.sh` UI log when available so it can show the actual Vite URL more explicitly.
- if `subscriber` exits, `start-all.sh` prints a large warning block and intentionally leaves `api` and `ui` running.
- if `api` or `ui` exits, `start-all.sh` still stops the remaining processes intentionally and reports which component failed plus where the combined logs were captured.
